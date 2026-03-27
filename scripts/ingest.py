"""
PharmaSub — OpenFDA Data Ingestion Script
Fetches real drug data from OpenFDA and seeds ChromaDB for the RAG pipeline.

HIPAA note: Only public drug data is ingested. No patient data is ever stored.
"""

import os
import sys
import time
import hashlib
import math
import json
import logging
import requests

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [INGEST] %(levelname)s %(message)s"
)
log = logging.getLogger(__name__)

CHROMA_HOST = os.getenv("CHROMA_HOST", "localhost")
CHROMA_PORT = os.getenv("CHROMA_PORT", "8000")
CHROMA_URL = f"http://{CHROMA_HOST}:{CHROMA_PORT}"
COLLECTION_NAME = "pharma_drugs"

OPENFDA_URL = os.getenv("OPENFDA_BASE_URL", "https://api.fda.gov/drug")

DRUG_QUERIES = [
    "statin cholesterol",
    "beta blocker hypertension",
    "ACE inhibitor blood pressure",
    "antibiotic infection",
    "proton pump inhibitor acid reflux",
    "antidepressant SSRI",
    "antihistamine allergy",
    "blood thinner anticoagulant",
    "calcium channel blocker",
    "diuretic water pill",
]


def wait_for_chroma():
    """Wait until ChromaDB is ready."""
    log.info("Waiting for ChromaDB at %s...", CHROMA_URL)
    for attempt in range(30):
        try:
            r = requests.get(f"{CHROMA_URL}/api/v1/heartbeat", timeout=5)
            if r.status_code == 200:
                log.info("ChromaDB is ready.")
                return True
        except Exception:
            pass
        time.sleep(2)
    log.error("ChromaDB did not become ready in time.")
    return False


def get_or_create_collection():
    """Create ChromaDB collection or return existing one."""
    # Try to create
    r = requests.post(f"{CHROMA_URL}/api/v1/collections", json={
        "name": COLLECTION_NAME,
        "metadata": {"description": "PharmaSub drug knowledge base"}
    })
    if r.status_code in (200, 201):
        coll = r.json()
        log.info("Created collection: %s (id=%s)", COLLECTION_NAME, coll.get("id"))
        return coll["id"]

    # Fetch existing
    r = requests.get(f"{CHROMA_URL}/api/v1/collections/{COLLECTION_NAME}")
    r.raise_for_status()
    coll = r.json()
    log.info("Using existing collection: %s (id=%s)", COLLECTION_NAME, coll.get("id"))
    return coll["id"]


def get_document_count(collection_id):
    try:
        r = requests.get(f"{CHROMA_URL}/api/v1/collections/{collection_id}/count")
        return r.json().get("count", 0)
    except Exception:
        return 0


def embed_text(text: str) -> list:
    """
    Deterministic pseudo-embedding (384-dim).
    For production: replace with voyage-3 or text-embedding-ada-002.
    Must match the Java ClaudeService.generateDemoEmbedding() logic.
    """
    dimensions = 384
    vector = [0.0] * dimensions
    normalized = "".join(c if c.isalnum() or c == " " else "" for c in text.lower())
    tokens = normalized.split()
    if not tokens:
        tokens = ["unknown"]

    for token in tokens:
        h = int(hashlib.md5(token.encode()).hexdigest(), 16)
        # Make it signed 32-bit range
        h = h & 0xFFFFFFFF
        if h >= 0x80000000:
            h -= 0x100000000
        for i in range(dimensions):
            vector[i] += math.sin((h + i) * 0.1) / len(tokens)

    # Normalize to unit length
    norm = math.sqrt(sum(v * v for v in vector))
    if norm > 0:
        vector = [v / norm for v in vector]

    return vector


def build_embedding_text(drug: dict) -> str:
    return (
        f"Drug: {drug.get('brand_name', drug.get('generic_name', 'Unknown'))}. "
        f"Generic name: {drug.get('generic_name', 'unknown')}. "
        f"Class: {drug.get('drug_class', 'unknown')}. "
        f"Pharmacologic class: {drug.get('pharmacologic_class', 'unknown')}. "
        f"Route: {drug.get('route', 'oral')}. "
        f"Active ingredients: {drug.get('generic_name', 'unknown')}. "
        f"Interactions: {drug.get('interactions', 'see prescribing information')}. "
        f"Contraindications: {drug.get('contraindications', 'see prescribing information')}. "
        f"Allergy flags: {drug.get('allergy_flags', 'none')}."
    )


def fetch_openfda_drugs(query: str, limit: int = 8) -> list:
    """Fetch drug labels from OpenFDA API."""
    try:
        url = f"{OPENFDA_URL}/label.json"
        params = {
            "search": f"pharmacological_class:\"{query}\"",
            "limit": limit
        }
        r = requests.get(url, params=params, timeout=15)
        if r.status_code != 200:
            return []

        results = r.json().get("results", [])
        drugs = []
        for label in results:
            meta = label.get("openfda", {})
            generic = meta.get("generic_name", ["Unknown"])[0] if meta.get("generic_name") else "Unknown"
            brand = meta.get("brand_name", [generic])[0] if meta.get("brand_name") else generic
            ndc = meta.get("product_ndc", [None])[0] if meta.get("product_ndc") else None

            if not ndc:
                ndc = hashlib.md5(generic.encode()).hexdigest()[:11]

            interactions_raw = label.get("drug_interactions", ["see prescribing information"])
            contraindications_raw = label.get("contraindications", ["see prescribing information"])
            pharm_class = meta.get("pharm_class_epc", [query])

            # Extract allergy flags from contraindications
            contra_text = " ".join(contraindications_raw).lower()
            allergy_flags = []
            if "sulfa" in contra_text or "sulfonamide" in contra_text:
                allergy_flags.append("sulfa")
            if "penicillin" in contra_text:
                allergy_flags.append("penicillin")
            if "shellfish" in contra_text or "iodine" in contra_text:
                allergy_flags.append("shellfish")
            if "aspirin" in contra_text or "nsaid" in contra_text:
                allergy_flags.append("aspirin")
            if not allergy_flags:
                allergy_flags = ["none"]

            drug = {
                "id": f"fda-{ndc.replace('/', '-')}",
                "generic_name": generic,
                "brand_name": brand,
                "drug_class": query,
                "pharmacologic_class": pharm_class[0] if pharm_class else query,
                "route": label.get("route", ["oral"])[0] if label.get("route") else "oral",
                "interactions": interactions_raw[0][:300] if interactions_raw else "see prescribing information",
                "contraindications": contraindications_raw[0][:300] if contraindications_raw else "see prescribing information",
                "allergy_flags": ", ".join(allergy_flags),
                "ndc": ndc,
            }
            drugs.append(drug)

        return drugs
    except Exception as e:
        log.warning("OpenFDA fetch failed for '%s': %s", query, e)
        return []


def get_fallback_drugs() -> list:
    """Hardcoded fallback if OpenFDA is unreachable."""
    return [
        {"id": "mock-atorvastatin", "generic_name": "Atorvastatin", "brand_name": "Lipitor",
         "drug_class": "statin", "pharmacologic_class": "HMG-CoA reductase inhibitor",
         "route": "oral", "interactions": "warfarin, cyclosporine, clarithromycin",
         "contraindications": "active liver disease, pregnancy", "allergy_flags": "none", "ndc": "0071-0155-23"},
        {"id": "mock-rosuvastatin", "generic_name": "Rosuvastatin", "brand_name": "Crestor",
         "drug_class": "statin", "pharmacologic_class": "HMG-CoA reductase inhibitor",
         "route": "oral", "interactions": "warfarin, antacids, lopinavir",
         "contraindications": "active liver disease, pregnancy", "allergy_flags": "none", "ndc": "0310-0755-90"},
        {"id": "mock-pravastatin", "generic_name": "Pravastatin", "brand_name": "Pravachol",
         "drug_class": "statin", "pharmacologic_class": "HMG-CoA reductase inhibitor",
         "route": "oral", "interactions": "cyclosporine, clarithromycin",
         "contraindications": "active liver disease", "allergy_flags": "none", "ndc": "0003-0154-58"},
        {"id": "mock-amoxicillin", "generic_name": "Amoxicillin", "brand_name": "Amoxil",
         "drug_class": "antibiotic", "pharmacologic_class": "penicillin",
         "route": "oral", "interactions": "warfarin, methotrexate",
         "contraindications": "penicillin allergy", "allergy_flags": "penicillin", "ndc": "0093-3107-01"},
        {"id": "mock-azithromycin", "generic_name": "Azithromycin", "brand_name": "Zithromax",
         "drug_class": "antibiotic", "pharmacologic_class": "macrolide",
         "route": "oral", "interactions": "warfarin, digoxin",
         "contraindications": "macrolide allergy", "allergy_flags": "none", "ndc": "0069-3060-20"},
        {"id": "mock-metoprolol", "generic_name": "Metoprolol", "brand_name": "Lopressor",
         "drug_class": "beta blocker", "pharmacologic_class": "beta-1 selective adrenergic blocker",
         "route": "oral", "interactions": "verapamil, clonidine, digoxin",
         "contraindications": "severe bradycardia, heart block", "allergy_flags": "none", "ndc": "0078-0378-05"},
        {"id": "mock-lisinopril", "generic_name": "Lisinopril", "brand_name": "Zestril",
         "drug_class": "ACE inhibitor", "pharmacologic_class": "angiotensin-converting enzyme inhibitor",
         "route": "oral", "interactions": "NSAIDs, potassium supplements, lithium",
         "contraindications": "angioedema history, pregnancy", "allergy_flags": "none", "ndc": "0310-0130-10"},
        {"id": "mock-omeprazole", "generic_name": "Omeprazole", "brand_name": "Prilosec",
         "drug_class": "proton pump inhibitor", "pharmacologic_class": "proton pump inhibitor",
         "route": "oral", "interactions": "clopidogrel, methotrexate, warfarin",
         "contraindications": "hypersensitivity to PPIs", "allergy_flags": "none", "ndc": "0186-0742-31"},
    ]


def add_to_chroma(collection_id: str, drugs: list):
    """Add drug documents with embeddings to ChromaDB."""
    if not drugs:
        return

    ids = [d["id"] for d in drugs]
    texts = [build_embedding_text(d) for d in drugs]
    embeddings = [embed_text(t) for t in texts]
    metadatas = [{
        "genericName": d.get("generic_name", ""),
        "brandName": d.get("brand_name", ""),
        "drugClass": d.get("drug_class", ""),
        "ndc": d.get("ndc", ""),
    } for d in drugs]

    payload = {
        "ids": ids,
        "embeddings": embeddings,
        "documents": texts,
        "metadatas": metadatas,
    }

    r = requests.post(
        f"{CHROMA_URL}/api/v1/collections/{collection_id}/add",
        json=payload,
        timeout=30
    )
    if r.status_code in (200, 201):
        log.info("Added %d documents to ChromaDB", len(drugs))
    else:
        log.warning("ChromaDB add returned %d: %s", r.status_code, r.text[:200])


def main():
    if not wait_for_chroma():
        sys.exit(1)

    collection_id = get_or_create_collection()
    existing = get_document_count(collection_id)

    if existing > 0:
        log.info("ChromaDB already has %d documents. Skipping ingestion.", existing)
        return

    log.info("Starting drug data ingestion...")
    total = 0
    all_drugs = []

    for query in DRUG_QUERIES:
        log.info("Fetching drugs for query: '%s'", query)
        drugs = fetch_openfda_drugs(query, limit=8)
        if drugs:
            all_drugs.extend(drugs)
            total += len(drugs)
            log.info("  Got %d drugs from OpenFDA", len(drugs))
        else:
            log.info("  OpenFDA returned no results, will use fallback")
        time.sleep(0.3)  # Rate limit

    if total == 0:
        log.warning("OpenFDA returned no data. Using fallback drug dataset.")
        all_drugs = get_fallback_drugs()

    # De-duplicate by id
    seen = set()
    unique_drugs = []
    for d in all_drugs:
        if d["id"] not in seen:
            seen.add(d["id"])
            unique_drugs.append(d)

    # Ingest in batches of 10
    batch_size = 10
    for i in range(0, len(unique_drugs), batch_size):
        batch = unique_drugs[i:i + batch_size]
        add_to_chroma(collection_id, batch)

    log.info("Ingestion complete. Total unique documents: %d", len(unique_drugs))


if __name__ == "__main__":
    main()

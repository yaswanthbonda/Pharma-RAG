/**
 * @typedef {Object} DrugRecommendation
 * @property {string} drugName
 * @property {string} genericName
 * @property {string} brandName
 * @property {string} dosage
 * @property {string} drugClass
 * @property {number} formularyTier
 * @property {string} memberCopay
 * @property {string} equivalencyNote
 * @property {'PASSED'|'FAILED'|'N/A'} allergyCheckResult
 * @property {'PASSED'|'FAILED'|'N/A'} interactionCheckResult
 * @property {boolean} requiresPrescriberApproval
 * @property {string|null} prescriberApprovalReason
 */

/**
 * @typedef {Object} CandidateEvaluation
 * @property {string} drugName
 * @property {boolean} equivalencyPassed
 * @property {boolean} formularyPassed
 * @property {boolean} interactionPassed
 * @property {boolean} allergyPassed
 * @property {string|null} exclusionReason
 */

/**
 * @typedef {Object} RagMetadata
 * @property {number} documentsRetrieved
 * @property {number} topSimilarityScore
 * @property {string} vectorCollection
 * @property {number} retrievalTimeMs
 * @property {number} llmTimeMs
 */

/**
 * @typedef {Object} SubstitutionResponse
 * @property {'FOUND'|'NO_SAFE_SUBSTITUTE'|'NO_COVERED_SUBSTITUTE'|'UNIQUE_DRUG_NO_EQUIVALENT'} status
 * @property {string} reason
 * @property {DrugRecommendation|null} primaryRecommendation
 * @property {DrugRecommendation|null} conditionalOption
 * @property {CandidateEvaluation[]} evaluatedCandidates
 * @property {number} confidence
 * @property {string[]} sourceDocuments
 * @property {string|null} escalationQueue
 * @property {boolean} prescriberContactRequired
 * @property {string} generatedAt
 * @property {RagMetadata} ragMetadata
 */

/**
 * @typedef {Object} SubstitutionRequest
 * @property {string} drugName
 * @property {string} [dosage]
 * @property {string} memberId
 * @property {string} [planId]
 * @property {string} [drugClass]
 * @property {'OUT_OF_STOCK'|'COST_REDUCTION'|'FORMULARY_CHANGE'|'PATIENT_PREFERENCE'} reason
 * @property {string[]} [allergies]
 * @property {string[]} [currentMedications]
 */

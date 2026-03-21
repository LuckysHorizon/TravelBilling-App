package com.travelbillpro.service;

/**
 * Mutable context object passed through the extraction pipeline to accumulate
 * audit data from each step. Written to the DB by ExtractionAuditService at the end.
 *
 * This is a plain POJO — no Spring, no JPA, no transactions.
 * It serves as the data carrier between AI extraction steps and final audit recording.
 */
public class AuditContext {

    private final String filename;
    private String  pdfStructure;
    private String  modelUsed;
    private String  status       = "STARTED";
    private String  errorMessage;
    private Long    processingMs;
    private Integer promptTokens    = 0;
    private Integer completionTokens = 0;
    private final StringBuilder rawAiResponseBuilder = new StringBuilder();

    public AuditContext(String filename) {
        this.filename = filename;
    }

    /**
     * Absorb token usage and raw response from a NvidiaCallResult.
     * Called after each AI extraction step to accumulate data.
     */
    public void absorb(NvidiaExtractionService.ExtractionCallResult result) {
        if (result == null) return;
        this.modelUsed         = result.modelUsed();
        this.promptTokens     += result.promptTokens();
        this.completionTokens += result.completionTokens();
        appendRawResponse(result.rawJson());
    }

    /**
     * Append raw AI response.
     * Multiple AI calls in group booking accumulate here with boundaries.
     */
    public void appendRawResponse(String raw) {
        if (raw == null) return;
        if (rawAiResponseBuilder.length() > 0)
            rawAiResponseBuilder.append("\n---CALL BOUNDARY---\n");
        rawAiResponseBuilder.append(raw);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and Setters
    // ─────────────────────────────────────────────────────────────────────────

    public String  getFilename()         { return filename; }
    public String  getPdfStructure()     { return pdfStructure; }
    public void    setPdfStructure(String s) { this.pdfStructure = s; }
    
    public String  getModelUsed()        { return modelUsed; }
    public void    setModelUsed(String m)    { this.modelUsed = m; }
    
    public String  getStatus()           { return status; }
    public void    setStatus(String s)   { this.status = s; }
    
    public String  getErrorMessage()     { return errorMessage; }
    public void    setErrorMessage(String e) { this.errorMessage = e; }
    
    public Long    getProcessingMs()     { return processingMs; }
    public void    setProcessingMs(Long ms) { this.processingMs = ms; }
    
    public Integer getPromptTokens()     { return promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    
    public String  getRawAiResponse()    { return rawAiResponseBuilder.toString(); }
}

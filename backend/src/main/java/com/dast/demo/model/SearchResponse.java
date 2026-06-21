package com.dast.demo.model;

/**
 * =========================================================
 * MODULE 2: THE DATA MODEL (Response Object)
 * =========================================================
 *
 * This is a plain Java class (POJO) that represents the JSON
 * response our API sends back to the client.
 *
 * When Spring sees a method annotated with @ResponseBody
 * (included in @RestController), it uses Jackson to automatically
 * convert this object to JSON.
 *
 * Example: new SearchResponse("hello") → {"result": "hello"}
 *
 * Jackson serializes field names as-is, so:
 *   private String result  →  "result": "..."  in JSON
 */
public class SearchResponse {

    // The single field in our response JSON
    private String result;

    /**
     * Constructor: create a response with the given result text.
     * @param result the message to include in the JSON response
     */
    public SearchResponse(String result) {
        this.result = result;
    }

    /**
     * IMPORTANT: Jackson requires a getter to serialize this field to JSON.
     * Without getResult(), the JSON output would be: {}  (empty!)
     *
     * Naming convention: get + FieldName (capitalized) = getter
     * So for field "result", the getter is "getResult"
     */
    public String getResult() {
        return result;
    }

    /**
     * Setter is good practice even if not strictly required here.
     * It's needed if Jackson needs to deserialize (read) JSON into this class.
     */
    public void setResult(String result) {
        this.result = result;
    }
}

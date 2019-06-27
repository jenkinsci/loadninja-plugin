package com.smartbear.loadninja.api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import com.smartbear.loadninja.helper.TestSummary;

import java.util.Map;
import java.util.HashMap;

public class SimpleAPI {
  public static boolean validateAPIKey(String apiKey){
    try {
      HttpResponse<JsonNode> jsonResponse = Unirest
      .get("https://ldsj2iwi73.execute-api.us-east-1.amazonaws.com/prd/authcheck")
      .headers(getHeaders(apiKey))
      .asJson();
      if( jsonResponse.getStatus() == 200) {
        return true;
      } else {
        return false;
      }
    } catch (UnirestException ue){
      throw new RuntimeException(ue);
    }
  }

  public static String getScenarioFromId(String apiKey, String scenarioId ){
    try {
      HttpResponse<JsonNode> jsonResponse = Unirest
      .get("https://ldsj2iwi73.execute-api.us-east-1.amazonaws.com/prd/scenario/" + scenarioId)
      .headers(getHeaders(apiKey))
      .asJson();
      if( jsonResponse.getStatus() == 200) {
        String data = jsonResponse.getBody().getObject().getJSONObject("data").toString();
        return data;
      } else {
        throw new RuntimeException("Error getting scenario: " + jsonResponse.getBody());
      }
    } catch (UnirestException ue){
      throw new RuntimeException(ue);
    }
  }


  public static String runTest(String apiKey, String scenario ){
    try {
      HttpResponse<JsonNode> jsonResponse = Unirest
      .post("https://ldsj2iwi73.execute-api.us-east-1.amazonaws.com/prd/test-run")
      .headers(getHeaders(apiKey))
      .body(scenario)
      .asJson();
      if( jsonResponse.getStatus() == 200) {
        String testId = jsonResponse.getBody().getObject().getJSONObject("data").getString("testId");
        return testId;
      } else {
        throw new RuntimeException("Error starting test: " + jsonResponse.getBody());
      }
    } catch (UnirestException ue){
      throw new RuntimeException(ue);
    }
  }

  private static Map<String, String> getHeaders(String apiKey) {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("accept", "application/json");
    headers.put("Content-Type", "application/json");
    headers.put("Authorization", apiKey);

    return headers;
  }

  public static String getTestStatus(String apiKey, String testId){
    try {
      HttpResponse<JsonNode> jsonResponse = Unirest
      .get("https://ldsj2iwi73.execute-api.us-east-1.amazonaws.com/prd/test-run/" + testId + "/status")
      .headers(getHeaders(apiKey))
      .asJson();
      if( jsonResponse.getStatus() == 200) {
        String status = jsonResponse.getBody().getObject().getJSONObject("data").getString("status");
        return status;
      } else {
        throw new RuntimeException("Error getting status of test: " + jsonResponse.getBody());
      }
    } catch (UnirestException ue){
      throw new RuntimeException(ue);
    }
  }

  public static String getTestSummary(String apiKey, String testId){
    try {
      HttpResponse<JsonNode> jsonResponse = Unirest
      .get("https://ldsj2iwi73.execute-api.us-east-1.amazonaws.com/prd/test-run/" + testId + "/summary?lastRow=true")
      .headers(getHeaders(apiKey))
      .asJson();
      if( jsonResponse.getStatus() == 200) {
        try {
          return jsonResponse.getBody().getObject().getJSONObject("data").toString().replaceAll("\"", "").replace("{", "").replace("}", "");
        } catch (Exception e) {
          return "No test summary results available yet.";
        }
      } else {
        throw new RuntimeException("Error getting summary of test: " + jsonResponse.getBody());
      }
    } catch (UnirestException ue){
      throw new RuntimeException(ue);
    }
  }

  public static TestSummary getFinalTestSummary(String apiKey, String testId){
    try {
      HttpResponse<JsonNode> jsonResponse = Unirest
      .get("https://ldsj2iwi73.execute-api.us-east-1.amazonaws.com/prd/test-run/" + testId + "/summary?lastRow=true")
      .headers(getHeaders(apiKey))
      .asJson();
      if( jsonResponse.getStatus() == 200) {
        try {
          int totalSteps = jsonResponse.getBody().getObject().getJSONObject("data").getInt("totalSteps");
          int passedStepCount = jsonResponse.getBody().getObject().getJSONObject("data").getInt("passedSteps");
          int failedStepCount = jsonResponse.getBody().getObject().getJSONObject("data").getInt("failedSteps");
          int avgResponseTime = jsonResponse.getBody().getObject().getJSONObject("data").getInt("avgResponseTimeMS");

          TestSummary ts = new TestSummary(totalSteps, passedStepCount, failedStepCount, avgResponseTime);

          return ts;
        } catch (Exception e) {
          return new TestSummary(-1, -1, -1, -1);
        }
      } else {
        throw new RuntimeException("Error getting summary of test: " + jsonResponse.getBody());
      }
    } catch (UnirestException ue){
      throw new RuntimeException(ue);
    }
  }
}

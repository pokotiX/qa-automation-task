package com.qa.taskapi.test.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringJUnitConfig
public class TaskApiTest {

    @LocalServerPort
    private int port;

    private static final String BASE_URL = "http://localhost";
    private static final String API_PATH = "/api/tasks";
    private static final String STATUS_TODO = "TODO";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_HIGH = "HIGH";

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = BASE_URL + ":" + port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    public void cleanUp() {
        Response response = RestAssured.given().get(API_PATH);
        Object[] tasks = response.getBody().as(Object[].class);
        for (Object obj : tasks) {
            Map task = (Map) obj;
            Long id = Long.valueOf(task.get("id").toString());
            RestAssured.given().delete(API_PATH + "/" + id);
        }
    }

    @Test
    public void shouldReturnHealthyStatus() {
        Response response = RestAssured.given()
                .when()
                .get(API_PATH + "/health");

        assertEquals(200, response.getStatusCode());
        assertEquals("Task API is running!", response.getBody().asString());
    }

    @Test
    public void shouldCreateNewTask() {
        Map<String, Object> request = createValidTaskRequest();

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(API_PATH);

        assertEquals(201, response.getStatusCode());

        Map createdTask = response.getBody().as(Map.class);
        assertNotNull(createdTask.get("id"));
        assertEquals(createdTask.get("title"), request.get("title"));
        assertEquals(createdTask.get("priority"), request.get("priority"));
        assertEquals(createdTask.get("status"), request.get("status"));
    }

    @Test
    public void shouldRetrieveAllTasks() {
        Map<String, Object> request = createValidTaskRequest();
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(API_PATH);

        Response response = RestAssured.given()
                .when()
                .get(API_PATH);

        assertEquals(200, response.getStatusCode());

        Object[] tasks = response.getBody().as(Object[].class);
        assertNotNull(tasks);
        assertTrue(tasks.length > 0);
    }

    @Test
    public void shouldRetrieveTaskById() {
        Map<String, Object> taskRequest = createValidTaskRequest();
        Response createResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(taskRequest)
                .when()
                .post(API_PATH);

        Map createdTask = createResponse.getBody().as(Map.class);
        Long taskId = Long.valueOf(createdTask.get("id").toString());

        Response response = RestAssured.given()
                .when()
                .get(API_PATH + "/" + taskId);

        assertEquals(200, response.getStatusCode());

        Map retrievedTask = response.getBody().as(Map.class);
        assertEquals(Long.valueOf(retrievedTask.get("id").toString()), taskId);
        assertEquals(retrievedTask.get("title"), taskRequest.get("title"));
    }

    @Test
    public void shouldReturnNotFoundForInvalidId() {
        Response response = RestAssured.given()
                .when()
                .get(API_PATH + "/99999");

        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void shouldUpdateExistingTask() {
        Map<String, Object> taskRequest = createValidTaskRequest();
        Response createResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(taskRequest)
                .when()
                .post(API_PATH);

        Map createdTask = createResponse.getBody().as(Map.class);
        long taskId = Long.parseLong(createdTask.get("id").toString());

        Map<String, Object> updateRequest = new HashMap<>(taskRequest);
        updateRequest.put("title", "Updated Task Title");
        updateRequest.put("priority", PRIORITY_HIGH);

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put(API_PATH + "/" + taskId);

        assertEquals(200, response.getStatusCode());

        Map updatedTask = response.getBody().as(Map.class);
        assertEquals("Updated Task Title", updatedTask.get("title"));
        assertEquals(PRIORITY_HIGH, updatedTask.get("priority"));
    }

    @Test
    public void shouldDeleteTaskSuccessfully() {
        Map<String, Object> taskRequest = createValidTaskRequest();
        Response createResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(taskRequest)
                .when()
                .post(API_PATH);

        Map createdTask = createResponse.getBody().as(Map.class);
        long taskId = Long.parseLong(createdTask.get("id").toString());

        Response response = RestAssured.given()
                .when()
                .delete(API_PATH + "/" + taskId);

        assertEquals(204, response.getStatusCode());

        Response getResponse = RestAssured.given()
                .when()
                .get(API_PATH + "/" + taskId);

        assertEquals(404, getResponse.getStatusCode());
    }

    @Test
    public void shouldFilterTasksByStatus() {
        Map<String, Object> taskRequest = createValidTaskRequest();
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(taskRequest)
                .when()
                .post(API_PATH);

        Response response = RestAssured.given()
                .when()
                .get(API_PATH + "/status/TODO");

        assertEquals(200, response.getStatusCode());

        Object[] tasks = response.getBody().as(Object[].class);
        assertNotNull(tasks);
        assertTrue(tasks.length > 0);
    }

    @Test
    public void shouldFilterTasksByPriority() {
        Map<String, Object> taskRequest = createValidTaskRequest();
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(taskRequest)
                .when()
                .post(API_PATH);

        Response response = RestAssured.given()
                .when()
                .get(API_PATH + "/priority/MEDIUM");

        assertEquals(200, response.getStatusCode());

        Object[] tasks = response.getBody().as(Object[].class);
        assertNotNull(tasks);
        assertTrue(tasks.length > 0);
    }

    @Test
    public void shouldSearchTasksByTitle() {
        Map<String, Object> taskRequest = createValidTaskRequest();
        String searchTitle = "Search Test Task";
        taskRequest.put("title", searchTitle);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(taskRequest)
                .when()
                .post(API_PATH);

        Response response = RestAssured.given()
                .queryParam("title", "Search Test")
                .when()
                .get(API_PATH + "/search");

        assertEquals(200, response.getStatusCode());

        Object[] tasks = response.getBody().as(Object[].class);
        assertNotNull(tasks);
        assertTrue(tasks.length > 0);
    }

    @Test
    public void shouldReturnBadRequestForMissingRequiredFields() {
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("description", "Missing title, priority, and status");

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post(API_PATH);

        assertEquals(400, response.getStatusCode());
    }

    private Map<String, Object> createValidTaskRequest() {
        Map<String, Object> taskRequest = new HashMap<>();
        taskRequest.put("title", "Test Task " + System.nanoTime());
        taskRequest.put("description", "Test task for API validation");
        taskRequest.put("priority", PRIORITY_MEDIUM);
        taskRequest.put("status", STATUS_TODO);
        taskRequest.put("dueDate", LocalDateTime.now().plusDays(7).toString());
        return taskRequest;
    }
}

package com.niladri.vidal_task;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

@Component
public class StartupRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {

        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Generate Webhook
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> body = new HashMap<>();
        body.put("name", "Niladri Pal");
        body.put("regNo", "REG12347");   // ODD → Question 1
        body.put("email", "yourmail@gmail.com");

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, body, Map.class);

        String webhookUrl = (String) response.getBody().get("webhook");
        String accessToken = (String) response.getBody().get("accessToken");

        System.out.println("Webhook URL: " + webhookUrl);
        System.out.println("Access Token: " + accessToken);

        // Step 2: Final SQL (Question 1)
        String finalQuery = """
        SELECT d.DEPARTMENT_NAME,
               t.total_salary AS SALARY,
               CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME,
               TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE
        FROM (
            SELECT emp.EMP_ID,
                   emp.DEPARTMENT,
                   SUM(p.AMOUNT) AS total_salary
            FROM EMPLOYEE emp
            JOIN PAYMENTS p ON emp.EMP_ID = p.EMP_ID
            WHERE DAY(p.PAYMENT_TIME) <> 1
            GROUP BY emp.EMP_ID, emp.DEPARTMENT
        ) t
        JOIN (
            SELECT DEPARTMENT, MAX(total_salary) AS max_salary
            FROM (
                SELECT emp.EMP_ID,
                       emp.DEPARTMENT,
                       SUM(p.AMOUNT) AS total_salary
                FROM EMPLOYEE emp
                JOIN PAYMENTS p ON emp.EMP_ID = p.EMP_ID
                WHERE DAY(p.PAYMENT_TIME) <> 1
                GROUP BY emp.EMP_ID, emp.DEPARTMENT
            ) x
            GROUP BY DEPARTMENT
        ) m ON t.DEPARTMENT = m.DEPARTMENT AND t.total_salary = m.max_salary
        JOIN EMPLOYEE e ON t.EMP_ID = e.EMP_ID
        JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID;
        """;

        // Step 3: Submit Final Query
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> finalBody = new HashMap<>();
        finalBody.put("finalQuery", finalQuery);

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(finalBody, headers);

        ResponseEntity<String> finalResponse =
                restTemplate.postForEntity(webhookUrl, entity, String.class);

        System.out.println("Final Response: " + finalResponse.getBody());
    }
}

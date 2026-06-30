package com.shivankkapoor.standbase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shivankkapoor.standbase.config.SecurityConfig;
import com.shivankkapoor.standbase.dto.request.CreateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.request.ReorderTodosRequestDTO;
import com.shivankkapoor.standbase.dto.request.UpdateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.response.TodoSummaryResponseDTO;
import com.shivankkapoor.standbase.model.Todo;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
import com.shivankkapoor.standbase.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
@Import(SecurityConfig.class)
class TodoControllerTest {

    @Autowired WebApplicationContext wac;
    MockMvc mockMvc;

    @MockitoBean TodoService todoService;
    @MockitoBean IpService ipService;
    @MockitoBean SessionService sessionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TODO_ID = UUID.randomUUID();
    private static final String TOKEN = "test-session-token";
    private static final LocalDate DATE = LocalDate.of(2026, 5, 26);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(sessionService.getSessionUserID(eq(TOKEN), eq("1.2.3.4"))).thenReturn(USER_ID);
    }

    private Todo buildTodo() {
        Todo todo = new Todo();
        todo.setId(TODO_ID);
        todo.setUserId(USER_ID);
        todo.setEntryDate(DATE);
        todo.setContent("Write tests");
        todo.setCompleted(false);
        todo.setPosition(0);
        todo.setCreatedAt(OffsetDateTime.now());
        return todo;
    }

    @Test
    void createTodo_valid_returns200() throws Exception {
        when(todoService.createTodo(eq(USER_ID), any())).thenReturn(buildTodo());

        CreateTodoRequestDTO body = new CreateTodoRequestDTO();
        body.setEntryDate(DATE);
        body.setContent("Write tests");

        mockMvc.perform(post("/todos")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Write tests"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    void createTodo_missingContent_returns400() throws Exception {
        mockMvc.perform(post("/todos")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entryDate\":\"2026-05-26\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTodo_missingDate_returns400() throws Exception {
        mockMvc.perform(post("/todos")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Write tests\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTodo_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTodos_authenticated_returnsList() throws Exception {
        when(todoService.getTodos(USER_ID, DATE)).thenReturn(List.of(buildTodo()));

        mockMvc.perform(get("/todos?date=2026-05-26")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Write tests"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void getTodos_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/todos?date=2026-05-26"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTodo_found_returns200() throws Exception {
        Todo updated = buildTodo();
        updated.setCompleted(true);
        when(todoService.updateTodo(eq(TODO_ID), eq(USER_ID), any())).thenReturn(Optional.of(updated));

        UpdateTodoRequestDTO body = new UpdateTodoRequestDTO();
        body.setCompleted(true);

        mockMvc.perform(put("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void updateTodo_withEntryDate_returns200WithNewDate() throws Exception {
        LocalDate targetDate = DATE.plusDays(1);
        Todo moved = buildTodo();
        moved.setEntryDate(targetDate);
        moved.setPosition(0);
        when(todoService.updateTodo(eq(TODO_ID), eq(USER_ID), any())).thenReturn(Optional.of(moved));

        UpdateTodoRequestDTO body = new UpdateTodoRequestDTO();
        body.setEntryDate(targetDate);

        mockMvc.perform(put("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryDate").value("2026-05-27"))
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    void updateTodo_notFound_returns404() throws Exception {
        when(todoService.updateTodo(eq(TODO_ID), eq(USER_ID), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTodo_differentUser_returns404() throws Exception {
        UUID otherUser = UUID.randomUUID();
        when(sessionService.getSessionUserID(eq("other-token"), eq("1.2.3.4"))).thenReturn(otherUser);
        when(todoService.updateTodo(eq(TODO_ID), eq(otherUser), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer other-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reorderTodos_valid_returns200() throws Exception {
        List<UUID> ids = List.of(TODO_ID);
        when(todoService.reorderTodos(eq(USER_ID), eq(ids))).thenReturn(List.of(buildTodo()));

        ReorderTodosRequestDTO body = new ReorderTodosRequestDTO();
        body.setIds(ids);

        mockMvc.perform(put("/todos/reorder")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TODO_ID.toString()));
    }

    @Test
    void deleteTodo_found_returns204() throws Exception {
        when(todoService.deleteTodo(TODO_ID, USER_ID)).thenReturn(true);

        mockMvc.perform(delete("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTodo_notFound_returns404() throws Exception {
        when(todoService.deleteTodo(TODO_ID, USER_ID)).thenReturn(false);

        mockMvc.perform(delete("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTodo_differentUser_returns404() throws Exception {
        UUID otherUser = UUID.randomUUID();
        when(sessionService.getSessionUserID(eq("other-token"), eq("1.2.3.4"))).thenReturn(otherUser);
        when(todoService.deleteTodo(TODO_ID, otherUser)).thenReturn(false);

        mockMvc.perform(delete("/todos/" + TODO_ID)
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTodoSummary_authenticated_returnsSummary() throws Exception {
        TodoSummaryResponseDTO summary = new TodoSummaryResponseDTO();
        summary.setDate(DATE);
        summary.setAllCompleted(false);
        when(todoService.getTodoSummary(eq(USER_ID), any(), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/todos/summary?year=2026&month=5")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-05-26"))
                .andExpect(jsonPath("$[0].allCompleted").value(false));
    }

    @Test
    void getTodoSummary_emptyMonth_returnsEmptyList() throws Exception {
        when(todoService.getTodoSummary(eq(USER_ID), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/todos/summary?year=2026&month=5")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getTodoSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/todos/summary?year=2026&month=5"))
                .andExpect(status().isUnauthorized());
    }
}

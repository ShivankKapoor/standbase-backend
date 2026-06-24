package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.request.CreateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.request.ReorderTodosRequestDTO;
import com.shivankkapoor.standbase.dto.request.UpdateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.response.TodoResponseDTO;
import com.shivankkapoor.standbase.model.Todo;
import com.shivankkapoor.standbase.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping("")
    public ResponseEntity<TodoResponseDTO> createTodo(@Valid @RequestBody CreateTodoRequestDTO dto, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(toResponse(todoService.createTodo(userId, dto)));
    }

    @GetMapping("")
    public ResponseEntity<List<TodoResponseDTO>> getTodos(@RequestParam LocalDate date, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.getTodos(userId, date).stream().map(this::toResponse).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TodoResponseDTO> updateTodo(@PathVariable UUID id, @Valid @RequestBody UpdateTodoRequestDTO dto, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return todoService.updateTodo(id, userId, dto)
                .map(todo -> ResponseEntity.ok(toResponse(todo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<TodoResponseDTO>> reorderTodos(@Valid @RequestBody ReorderTodosRequestDTO dto, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(todoService.reorderTodos(userId, dto.getIds()).stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable UUID id, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return todoService.deleteTodo(id, userId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private TodoResponseDTO toResponse(Todo todo) {
        TodoResponseDTO response = new TodoResponseDTO();
        response.setId(todo.getId());
        response.setEntryDate(todo.getEntryDate());
        response.setContent(todo.getContent());
        response.setCompleted(todo.isCompleted());
        response.setPosition(todo.getPosition());
        return response;
    }
}

package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.CreateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.request.UpdateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.response.TodoSummaryResponseDTO;
import com.shivankkapoor.standbase.model.Todo;
import com.shivankkapoor.standbase.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TodoService {

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    public Todo createTodo(UUID userId, CreateTodoRequestDTO dto) {
        List<Todo> existing = todoRepository.findByUserIdAndEntryDateOrderByPositionAscCreatedAtAsc(userId, dto.getEntryDate());
        Todo todo = new Todo();
        todo.setUserId(userId);
        todo.setEntryDate(dto.getEntryDate());
        todo.setContent(dto.getContent());
        todo.setCompleted(false);
        todo.setPosition(existing.size());
        todo.setCreatedAt(OffsetDateTime.now());
        return todoRepository.save(todo);
    }

    public List<Todo> getTodos(UUID userId, LocalDate date) {
        return todoRepository.findByUserIdAndEntryDateOrderByPositionAscCreatedAtAsc(userId, date);
    }

    public Optional<Todo> updateTodo(UUID id, UUID userId, UpdateTodoRequestDTO dto) {
        return todoRepository.findById(id)
                .filter(t -> t.getUserId().equals(userId))
                .map(todo -> {
                    if (dto.getContent() != null) todo.setContent(dto.getContent());
                    if (dto.getCompleted() != null) todo.setCompleted(dto.getCompleted());
                    return todoRepository.save(todo);
                });
    }

    @Transactional
    public boolean deleteTodo(UUID id, UUID userId) {
        Optional<Todo> todo = todoRepository.findById(id).filter(t -> t.getUserId().equals(userId));
        if (todo.isEmpty()) return false;
        todoRepository.deleteById(id);
        return true;
    }

    public List<TodoSummaryResponseDTO> getTodoSummary(UUID userId, LocalDate from, LocalDate to) {
        return todoRepository.findByUserIdAndEntryDateBetween(userId, from, to)
                .stream()
                .collect(Collectors.groupingBy(Todo::getEntryDate))
                .entrySet().stream()
                .map(e -> {
                    TodoSummaryResponseDTO dto = new TodoSummaryResponseDTO();
                    dto.setDate(e.getKey());
                    dto.setAllCompleted(e.getValue().stream().allMatch(Todo::isCompleted));
                    return dto;
                })
                .sorted(Comparator.comparing(TodoSummaryResponseDTO::getDate))
                .toList();
    }

    @Transactional
    public List<Todo> reorderTodos(UUID userId, List<UUID> ids) {
        Map<UUID, Todo> todoMap = StreamSupport.stream(todoRepository.findAllById(ids).spliterator(), false)
                .filter(t -> t.getUserId().equals(userId))
                .collect(Collectors.toMap(Todo::getId, t -> t));
        List<Todo> toSave = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Todo todo = todoMap.get(ids.get(i));
            if (todo != null) {
                todo.setPosition(i);
                toSave.add(todo);
            }
        }
        List<Todo> saved = new ArrayList<>();
        todoRepository.saveAll(toSave).forEach(saved::add);
        return saved;
    }
}

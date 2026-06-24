package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.CreateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.request.UpdateTodoRequestDTO;
import com.shivankkapoor.standbase.dto.response.TodoSummaryResponseDTO;
import com.shivankkapoor.standbase.model.Todo;
import com.shivankkapoor.standbase.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TodoServiceTest {

    private TodoRepository todoRepository;
    private TodoService todoService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 5, 26);

    @BeforeEach
    void setUp() {
        todoRepository = mock(TodoRepository.class);
        todoService = new TodoService(todoRepository);
    }

    private Todo buildTodo(UUID id, UUID userId, int position) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setUserId(userId);
        todo.setEntryDate(DATE);
        todo.setContent("Task " + position);
        todo.setCompleted(false);
        todo.setPosition(position);
        todo.setCreatedAt(OffsetDateTime.now());
        return todo;
    }

    @Test
    void createTodo_firstTodo_positionIsZero() {
        when(todoRepository.findByUserIdAndEntryDateOrderByPositionAscCreatedAtAsc(USER_ID, DATE))
                .thenReturn(List.of());
        Todo saved = buildTodo(UUID.randomUUID(), USER_ID, 0);
        when(todoRepository.save(any())).thenReturn(saved);

        CreateTodoRequestDTO dto = new CreateTodoRequestDTO();
        dto.setEntryDate(DATE);
        dto.setContent("Task 0");

        Todo result = todoService.createTodo(USER_ID, dto);
        assertThat(result.getPosition()).isEqualTo(0);
    }

    @Test
    void createTodo_appendsAtEnd() {
        Todo existing = buildTodo(UUID.randomUUID(), USER_ID, 0);
        when(todoRepository.findByUserIdAndEntryDateOrderByPositionAscCreatedAtAsc(USER_ID, DATE))
                .thenReturn(List.of(existing));
        Todo saved = buildTodo(UUID.randomUUID(), USER_ID, 1);
        when(todoRepository.save(any())).thenAnswer(inv -> {
            Todo t = inv.getArgument(0);
            assertThat(t.getPosition()).isEqualTo(1);
            return saved;
        });

        CreateTodoRequestDTO dto = new CreateTodoRequestDTO();
        dto.setEntryDate(DATE);
        dto.setContent("Task 1");

        todoService.createTodo(USER_ID, dto);
        verify(todoRepository).save(any());
    }

    @Test
    void getTodos_returnsRepositoryResults() {
        List<Todo> todos = List.of(buildTodo(UUID.randomUUID(), USER_ID, 0));
        when(todoRepository.findByUserIdAndEntryDateOrderByPositionAscCreatedAtAsc(USER_ID, DATE))
                .thenReturn(todos);

        assertThat(todoService.getTodos(USER_ID, DATE)).isEqualTo(todos);
    }

    @Test
    void updateTodo_ownerCanUpdateContent() {
        UUID id = UUID.randomUUID();
        Todo todo = buildTodo(id, USER_ID, 0);
        when(todoRepository.findById(id)).thenReturn(Optional.of(todo));
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTodoRequestDTO dto = new UpdateTodoRequestDTO();
        dto.setContent("Updated content");

        Optional<Todo> result = todoService.updateTodo(id, USER_ID, dto);
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Updated content");
    }

    @Test
    void updateTodo_ownerCanToggleCompleted() {
        UUID id = UUID.randomUUID();
        Todo todo = buildTodo(id, USER_ID, 0);
        when(todoRepository.findById(id)).thenReturn(Optional.of(todo));
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTodoRequestDTO dto = new UpdateTodoRequestDTO();
        dto.setCompleted(true);

        Optional<Todo> result = todoService.updateTodo(id, USER_ID, dto);
        assertThat(result).isPresent();
        assertThat(result.get().isCompleted()).isTrue();
    }

    @Test
    void updateTodo_nullFieldsAreIgnored() {
        UUID id = UUID.randomUUID();
        Todo todo = buildTodo(id, USER_ID, 0);
        todo.setContent("Original");
        when(todoRepository.findById(id)).thenReturn(Optional.of(todo));
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateTodoRequestDTO dto = new UpdateTodoRequestDTO(); // both fields null

        Optional<Todo> result = todoService.updateTodo(id, USER_ID, dto);
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Original");
        assertThat(result.get().isCompleted()).isFalse();
    }

    @Test
    void updateTodo_differentUser_returnsEmpty() {
        UUID id = UUID.randomUUID();
        Todo todo = buildTodo(id, UUID.randomUUID(), 0); // owned by someone else
        when(todoRepository.findById(id)).thenReturn(Optional.of(todo));

        Optional<Todo> result = todoService.updateTodo(id, USER_ID, new UpdateTodoRequestDTO());
        assertThat(result).isEmpty();
        verify(todoRepository, never()).save(any());
    }

    @Test
    void updateTodo_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(todoRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Todo> result = todoService.updateTodo(id, USER_ID, new UpdateTodoRequestDTO());
        assertThat(result).isEmpty();
    }

    @Test
    void deleteTodo_ownerCanDelete_returnsTrue() {
        UUID id = UUID.randomUUID();
        Todo todo = buildTodo(id, USER_ID, 0);
        when(todoRepository.findById(id)).thenReturn(Optional.of(todo));

        assertThat(todoService.deleteTodo(id, USER_ID)).isTrue();
        verify(todoRepository).deleteById(id);
    }

    @Test
    void deleteTodo_differentUser_returnsFalse() {
        UUID id = UUID.randomUUID();
        Todo todo = buildTodo(id, UUID.randomUUID(), 0); // owned by someone else
        when(todoRepository.findById(id)).thenReturn(Optional.of(todo));

        assertThat(todoService.deleteTodo(id, USER_ID)).isFalse();
        verify(todoRepository, never()).deleteById(any());
    }

    @Test
    void deleteTodo_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(todoRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(todoService.deleteTodo(id, USER_ID)).isFalse();
        verify(todoRepository, never()).deleteById(any());
    }

    @Test
    void reorderTodos_reassignsPositionsInOrder() {
        UUID id0 = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        Todo todo0 = buildTodo(id0, USER_ID, 1);
        Todo todo1 = buildTodo(id1, USER_ID, 0);

        when(todoRepository.findById(id0)).thenReturn(Optional.of(todo0));
        when(todoRepository.findById(id1)).thenReturn(Optional.of(todo1));
        when(todoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Todo> result = todoService.reorderTodos(USER_ID, List.of(id0, id1));

        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(t -> t.getId().equals(id0)).findFirst().get().getPosition()).isEqualTo(0);
        assertThat(result.stream().filter(t -> t.getId().equals(id1)).findFirst().get().getPosition()).isEqualTo(1);
    }

    @Test
    void getTodoSummary_onlyReturnsDatesWithTodos() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        Todo todo = buildTodo(UUID.randomUUID(), USER_ID, 0);
        when(todoRepository.findByUserIdAndEntryDateBetween(USER_ID, from, to)).thenReturn(List.of(todo));

        List<TodoSummaryResponseDTO> result = todoService.getTodoSummary(USER_ID, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(DATE);
        assertThat(result.get(0).isAllCompleted()).isFalse();
    }

    @Test
    void getTodoSummary_allCompleted_whenAllTodosOnDayAreCompleted() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        Todo t1 = buildTodo(UUID.randomUUID(), USER_ID, 0);
        Todo t2 = buildTodo(UUID.randomUUID(), USER_ID, 1);
        t1.setCompleted(true);
        t2.setCompleted(true);
        when(todoRepository.findByUserIdAndEntryDateBetween(USER_ID, from, to)).thenReturn(List.of(t1, t2));

        List<TodoSummaryResponseDTO> result = todoService.getTodoSummary(USER_ID, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAllCompleted()).isTrue();
    }

    @Test
    void getTodoSummary_notAllCompleted_whenAnyTodoIsIncomplete() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        Todo t1 = buildTodo(UUID.randomUUID(), USER_ID, 0);
        Todo t2 = buildTodo(UUID.randomUUID(), USER_ID, 1);
        t1.setCompleted(true);
        t2.setCompleted(false);
        when(todoRepository.findByUserIdAndEntryDateBetween(USER_ID, from, to)).thenReturn(List.of(t1, t2));

        List<TodoSummaryResponseDTO> result = todoService.getTodoSummary(USER_ID, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAllCompleted()).isFalse();
    }

    @Test
    void getTodoSummary_groupsByDate() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        LocalDate otherDate = LocalDate.of(2026, 5, 28);

        Todo t1 = buildTodo(UUID.randomUUID(), USER_ID, 0); // DATE
        Todo t2 = buildTodo(UUID.randomUUID(), USER_ID, 0); // otherDate
        t2.setEntryDate(otherDate);
        t2.setCompleted(true);

        when(todoRepository.findByUserIdAndEntryDateBetween(USER_ID, from, to)).thenReturn(List.of(t1, t2));

        List<TodoSummaryResponseDTO> result = todoService.getTodoSummary(USER_ID, from, to);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(DATE);
        assertThat(result.get(1).getDate()).isEqualTo(otherDate);
    }

    @Test
    void getTodoSummary_emptyRange_returnsEmpty() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(todoRepository.findByUserIdAndEntryDateBetween(USER_ID, from, to)).thenReturn(List.of());

        assertThat(todoService.getTodoSummary(USER_ID, from, to)).isEmpty();
    }

    @Test
    void reorderTodos_skipsIdsBelongingToOtherUsers() {
        UUID ownId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Todo ownTodo = buildTodo(ownId, USER_ID, 0);
        Todo otherTodo = buildTodo(otherId, UUID.randomUUID(), 0);

        when(todoRepository.findById(ownId)).thenReturn(Optional.of(ownTodo));
        when(todoRepository.findById(otherId)).thenReturn(Optional.of(otherTodo));
        when(todoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Todo> result = todoService.reorderTodos(USER_ID, List.of(ownId, otherId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(ownId);
    }
}

package com.qa.taskapi.service;

import com.qa.taskapi.dto.TaskRequest;
import com.qa.taskapi.dto.TaskResponse;
import com.qa.taskapi.model.Task;
import com.qa.taskapi.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {
    
    private final TaskRepository taskRepository;
    
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }
    
    public TaskResponse createTask(TaskRequest taskRequest) {
        // Validate required fields
        if (taskRequest.getTitle() == null || taskRequest.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (taskRequest.getPriority() == null || taskRequest.getPriority().trim().isEmpty()) {
            throw new IllegalArgumentException("Priority is required");
        }
        if (taskRequest.getStatus() == null || taskRequest.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }
        
        Task task = new Task();
        task.setTitle(taskRequest.getTitle());
        task.setDescription(taskRequest.getDescription());
        task.setPriority(taskRequest.getPriority());
        task.setStatus(taskRequest.getStatus());
        task.setDueDate(taskRequest.getDueDate());
        
        Task savedTask = taskRepository.save(task);
        return convertToResponse(savedTask);
    }
    
    public List<TaskResponse> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public Optional<TaskResponse> getTaskById(Long id) {
        Optional<Task> task = taskRepository.findById(id);
        return task.map(this::convertToResponse);
    }
    
    public Optional<TaskResponse> updateTask(Long id, TaskRequest taskRequest) {
        Optional<Task> existingTask = taskRepository.findById(id);
        
        if (existingTask.isPresent()) {
            Task task = existingTask.get();
            task.setTitle(taskRequest.getTitle());
            task.setDescription(taskRequest.getDescription());
            task.setPriority(taskRequest.getPriority());
            task.setStatus(taskRequest.getStatus());
            task.setDueDate(taskRequest.getDueDate());
            task.setUpdatedAt(LocalDateTime.now());
            
            Task updatedTask = taskRepository.save(task);
            return Optional.of(convertToResponse(updatedTask));
        }
        
        return Optional.empty();
    }
    
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<TaskResponse> getTasksByStatus(String status) {
        List<Task> tasks = taskRepository.findByStatus(status);
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TaskResponse> getTasksByPriority(String priority) {
        List<Task> tasks = taskRepository.findByPriority(priority);
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TaskResponse> searchTasksByTitle(String title) {
        List<Task> tasks = taskRepository.findByTitleContainingIgnoreCase(title);
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    private TaskResponse convertToResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getDueDate()
        );
    }
}

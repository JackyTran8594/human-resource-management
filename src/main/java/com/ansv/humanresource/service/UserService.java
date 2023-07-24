package com.ansv.humanresource.service;

import com.ansv.humanresource.dto.response.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {
    UserDTO findById(Long id);

    UserDTO save(UserDTO item);

    UserDTO findByUsername(String username);

    List<UserDTO> findAll();

    List<UserDTO> search(Map<String, Object> mapParam);

    Page<UserDTO> findBySearchCriteria(Optional<String> search, Pageable page);

    void deleteById(Long id);

    Integer deleteByListId(List<Long> id);
}

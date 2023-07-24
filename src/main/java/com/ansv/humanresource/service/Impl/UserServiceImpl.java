package com.ansv.humanresource.service.Impl;

import com.ansv.humanresource.dto.mapper.BaseMapper;
import com.ansv.humanresource.dto.specification.GenericSpecificationBuilder;
import com.ansv.humanresource.model.UserEntity;
import com.ansv.humanresource.repository.UserEntityRepository;
import com.ansv.humanresource.service.UserService;
import com.ansv.humanresource.dto.criteria.SearchCriteria;
import com.ansv.humanresource.dto.response.UserDTO;
import com.ansv.humanresource.util.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final BaseMapper<UserEntity, UserDTO> mapper = new BaseMapper<>(UserEntity.class, UserDTO.class);


    @Autowired
    private UserEntityRepository repository;

    @Override
    public UserDTO findById(Long id) {
        if (DataUtils.notNull(id)) {
            Optional<UserEntity> entity = repository.findById(id);
            if (entity.isPresent()) {
                return mapper.toDtoBean(entity.get());
            }
        }
        return null;
    }

    @Override
    public UserDTO save(UserDTO item) {
//        try {
        UserEntity entity = null;

        UserDTO dto = findById(item.getId());
        if (DataUtils.notNull(dto)) {
            item.setLastModifiedDate(LocalDateTime.now());
        }
        entity = mapper.toPersistenceBean(item);
        return mapper.toDtoBean(repository.save(entity));
    }

    @Override
    public UserDTO findByUsername(String username) {
        if (DataUtils.notNull(username)) {
            UserEntity entity = repository.findByUsername(username);
            if (DataUtils.notNull(entity)) {
                return mapper.toDtoBean(entity);
            }
        }
        return null;
    }

    @Override
    public List<UserDTO> findAll() {
        List<UserEntity> entities = repository.findAll();
        return mapper.toDtoBean(entities);
    }

    @Override
    public List<UserDTO> search(Map<String, Object> mapParam) {
        return null;
    }

    @Override
    public Page<UserDTO> findBySearchCriteria(Optional<String> search, Pageable page) {
        GenericSpecificationBuilder<UserEntity> builder = new GenericSpecificationBuilder<>();

        // check chuỗi để tách các param search
        if (DataUtils.notNull(search)) {
            Pattern pattern = Pattern.compile("(\\w+?)(\\.)(:|<|>|(\\w+?))(\\.)(\\w+?),", Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(search + ",");
            while (matcher.find()) {
                builder.with(new SearchCriteria(matcher.group(1), matcher.group(3), matcher.group(6)));
            }
        }
        // specification
        builder.setClazz(UserEntity.class);
        Specification<UserEntity> spec = builder.build();
        Page<UserDTO> listDTO = repository.findAll(spec, page).map(entity -> {
            UserDTO dto = mapper.toDtoBean(entity);
            return dto;
        });
        return listDTO;
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Integer deleteByListId(List<Long> listId) {
        return repository.deleteByListId(listId);
    }
}

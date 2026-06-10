package com.aimsgraph.domain.user.mapper;

import com.aimsgraph.domain.user.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    void insertUser(Users user);

    Users findByUsername(@Param("username") String username);
}

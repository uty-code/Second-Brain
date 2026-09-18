# MyBatis 동적 SQL과 XML 매퍼 예시

복잡한 검색 조건에 따라 WHERE 절을 동적으로 생성할 때 MyBatis의 XML 태그를 활용합니다.

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.UserMapper">

  <select id="searchUsers" resultType="com.example.model.User">
    SELECT id, username, email, status, created_at
    FROM users
    <where>
      <if test="username != null and username != ''">
        AND username LIKE CONCAT('%', #{username}, '%')
      </if>
      <if test="status != null">
        AND status = #{status}
      </if>
      <if test="startDate != null and endDate != null">
        AND created_at BETWEEN #{startDate} AND #{endDate}
      </if>
    </where>
    ORDER BY id DESC
  </select>

</mapper>
```

`<where>` 태그는 내부의 하위 조건이 하나라도 참일 때만 `WHERE` 키워드를 자동으로 삽입하며, 맨 앞에 오는 불필요한 `AND`나 `OR`를 자동으로 제거하여 문법 에러를 방지합니다.

package com.example.demo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(path = "rest")
public class DemoRest {

	@Autowired
	StringRedisTemplate redisTemplate;
	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	ObjectMapper om;
	final static String SHOW_SQL="SELECT * FROM testtable WHERE id=?";
	final static String ALTER_SQL="UPDATE testtable SET name = ?, age =?  WHERE id = ?";

	@PostMapping(path = "create", produces = MediaType.APPLICATION_JSON_VALUE)
	public String create(@RequestBody Map<String, String> map) {
		TestObject testObject = new TestObject();
		String name =  map.get("name");
		Integer age = Integer.valueOf(map.get("age"));
		testObject.setAge(Integer.valueOf(map.get("age")));
		testObject.setName((String) map.get("name"));
		return "新增了 " + addTest(testObject) + " 筆資料";
	}

	@GetMapping(path = "show/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String showKv(@PathVariable("id") String id) throws JsonProcessingException {
		String obj = redisTemplate.opsForValue().get(id);
		if (obj != null) {
			System.out.println("redis output");
			return obj;
		}
		List<TestObject> testList = jdbcTemplate.query(SHOW_SQL, this::row2TestObject, id);
		String testListJson = om.writeValueAsString(testList);
		redisTemplate.opsForValue().set(id, testListJson);
		redisTemplate.expire(id, 10, TimeUnit.SECONDS);
		System.out.println("DB output");
		return testListJson;

	}

	@PutMapping(path = "alter", produces = MediaType.APPLICATION_JSON_VALUE)
	public String testUpdate(@RequestBody Map<String, String> map) {
		String name = (String) map.get("name");
		Integer age = Integer.valueOf(map.get("age"));
		Integer id = Integer.valueOf(map.get("id"));
		int i = jdbcTemplate.update(ALTER_SQL, name, age, id);
		redisTemplate.delete(String.valueOf(id));
		return "修改了 " + i + " 筆資料";
	}

	public int addTest(TestObject obj) {
		String sql = "INSERT INTO testTable(NAME,AGE) VALUE(?,?)";
		return jdbcTemplate.update(sql, obj.getName(), obj.getAge());
	}

	public TestObject row2TestObject(ResultSet rs, int rowNo) throws SQLException {
		TestObject testObject = new TestObject();
		String name = rs.getString("name");
		int age = rs.getInt("age");
		int id = rs.getInt("id");
		testObject.setAge(age);
		testObject.setName(name);
		testObject.setId(id);
		return testObject;
	}

}
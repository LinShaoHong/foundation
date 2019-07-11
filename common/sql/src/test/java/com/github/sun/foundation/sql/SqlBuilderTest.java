package com.github.sun.foundation.sql;

import com.github.sun.foundation.boot.utility.SqlFormatter;
import com.github.sun.foundation.sql.factory.SqlBuilderFactory;
import org.junit.Test;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @Author LinSH
 * @Date: 11:27 AM 2019-03-01
 */
public class SqlBuilderTest {
  private SqlBuilder.Factory factory = SqlBuilderFactory.postgres();

  @Table(name = "table_1")
  private static class Entity1 {
    @Id
    private String id;
    private String name;
    private String nickName;
    @Handler(AddressesHandler.class)
    private List<String> addresses;
    private String e2Id;
    private String e3Id;
    private int rank;
    private long time;
    @Handler(ObjectHandler.class)
    private Obj obj;
    @Transient
    @SelectAliasPrefix("entity4")
    private List<Entity4> list4;
    @Transient
    @SelectAliasPrefix("entity5")
    private Entity5 entity5;
  }

  private static class Obj {
    private final String name;
    private final int integer;
    private final long time;

    public Obj(String name, int integer, long time) {
      this.name = name;
      this.integer = integer;
      this.time = time;
    }

    @Override
    public String toString() {
      return "SqlBuilderTest.Obj(name=" + name + ", integer=" + integer + ", time=" + time + ")";
    }
  }

  @Table(name = "table_2")
  private static class Entity2 {
    @Id
    private String id;
    private String name;
    private String nickName;
    private long time;
    private int rank;
    @Handler(ObjectHandler.class)
    private Obj obj;
  }

  @Table(name = "table_3")
  private static class Entity3 {
    @Id
    private String id;
    private String name;
    private long time;
    private int rank;
    @Handler(ObjectHandler.class)
    private Obj obj;
  }

  @Table(name = "table_4")
  private static class Entity4 {
    @Id
    private String id;
    private String e1Id;
    private String name;
    private long time;
  }

  @Table(name = "table_5")
  private static class Entity5 {
    @Id
    private String id;
    private String e1Id;
    private String name;
    private long time;
  }

  public static class ObjectHandler {
  }

  public static class AddressesHandler {
  }

  @Test
  public void test_单表操作及基本关系查询() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class)
      .where(sb.field("id").eq("abc123"))
      .where(sb.field("name").eq("hello").or(sb.field("nickName").eq("world")))
      .where(sb.field("rank").isNotNull())
      .where(sb.field("time").lt(9876543))
      .where(sb.field("time").ge(5324534))
      .where(sb.field("obj.name").eq("obj name"))
      .where(sb.field("obj.integer").gt(25))
      .asc("rank")
      .desc("time")
      .limit(1, 100)
      .select("name", "nickName", "obj.name")
      .template();

    assertEquals(template.literalSQL(),
      "SELECT \"name\", \"nick_name\", \"obj\"->>'name' AS \"obj_name\" FROM \"table_1\" WHERE \"id\" = 'abc123' " +
        "AND (\"name\" = 'hello' OR \"nick_name\" = 'world') AND \"rank\" IS NOT NULL AND \"time\" < 9876543 " +
        "AND \"time\" >= 5324534 AND \"obj\"->>'name' = 'obj name' AND \"obj\"->>'integer' > 25 " +
        "ORDER BY \"rank\" ASC, \"time\" DESC LIMIT 100 OFFSET 1");
    assertEquals(template.parameterizedSQL(),
      "SELECT \"name\", \"nick_name\", \"obj\"->>'name' AS \"obj_name\" FROM \"table_1\" WHERE \"id\" = #{$1} " +
        "AND (\"name\" = #{$2} OR \"nick_name\" = #{$3}) AND \"rank\" IS NOT NULL AND \"time\" < #{$4} " +
        "AND \"time\" >= #{$5} AND \"obj\"->>'name' = #{$6} AND \"obj\"->>'integer' > #{$7} " +
        "ORDER BY \"rank\" ASC, \"time\" DESC LIMIT #{$8} OFFSET #{$9}");
    assertEquals(template.placeholderSQL(),
      "SELECT \"name\", \"nick_name\", \"obj\"->>'name' AS \"obj_name\" FROM \"table_1\" WHERE \"id\" = ? " +
        "AND (\"name\" = ? OR \"nick_name\" = ?) AND \"rank\" IS NOT NULL AND \"time\" < ? AND \"time\" >= ? " +
        "AND \"obj\"->>'name' = ? AND \"obj\"->>'integer' > ? " +
        "ORDER BY \"rank\" ASC, \"time\" DESC LIMIT ? OFFSET ?");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "abc123");
    assertEquals(parameters.get("$2"), "hello");
    assertEquals(parameters.get("$3"), "world");
    assertEquals(parameters.get("$4"), 9876543);
    assertEquals(parameters.get("$5"), 5324534);
    assertEquals(parameters.get("$6"), "obj name");
    assertEquals(parameters.get("$7"), 25);
    assertEquals(parameters.get("$8"), 100);
    assertEquals(parameters.get("$9"), 1);
  }

  @Test
  public void test_多表连接() {
    long time = System.currentTimeMillis();
    Obj obj = new Obj("lin", 123, time);

    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class, "e1")
      .leftJoin(Entity2.class, "e2", sb.field("e1.e2Id").eq(sb.field("e2.id")))
      .innerJoin(Entity3.class, "e3", sb.field("e1.e3Id").eq(sb.field("e3.id")))
      .rightJoin(Entity4.class, "e4", sb.field("e1.id").eq(sb.field("e4.e1Id")))
      .leftJoin(Entity5.class, "e5", sb.field("e1.id").eq(sb.field("e5.e1Id")))
      .innerJoin("dims_table", "dims", sb.field("dims.e5Id").eq(sb.field("e5.id")))
      .where(sb.field("e1.name").eq("hello").or(sb.field("e1.nickName").eq("world")))
      .where(sb.field("e2.obj").isNotNull())
      .where(sb.field("e3.time").lt(9876543))
      .where(sb.field("e3.time").ge(5324534))
      .where(sb.field("e1.obj.name").eq("obj name"))
      .where(sb.field("e2.obj.integer").gt(25))
      .where(sb.field("e3.obj").eq(obj))
      .asc("e1.time")
      .desc("e3.time")
      .limit(10, 100)
      .select("e1.name", "e1.obj.name", "e2.nickName", "e3.time", "e4.name", "e4.time", "e5.*")
      .template();

    assertEquals(template.literalSQL(),
      "SELECT \"e1\".\"name\" AS \"name\", \"e1\".\"obj\"->>'name' AS \"obj_name\", \"e2\".\"nick_name\" AS \"nick_name\", \"e3\".\"time\" AS \"time\", " +
        "\"e4\".\"name\" AS \"entity4_name\", \"e4\".\"time\" AS \"entity4_time\", \"e5\".\"id\" AS \"entity5_id\", \"e5\".\"e1_id\" AS \"entity5e1_id\", " +
        "\"e5\".\"name\" AS \"entity5_name\", \"e5\".\"time\" AS \"entity5_time\" FROM \"table_1\" AS \"e1\" " +
        "LEFT JOIN \"table_2\" AS \"e2\" ON \"e1\".\"e2_id\" = \"e2\".\"id\" " +
        "INNER JOIN \"table_3\" AS \"e3\" ON \"e1\".\"e3_id\" = \"e3\".\"id\" " +
        "RIGHT JOIN \"table_4\" AS \"e4\" ON \"e1\".\"id\" = \"e4\".\"e1_id\" " +
        "LEFT JOIN \"table_5\" AS \"e5\" ON \"e1\".\"id\" = \"e5\".\"e1_id\" " +
        "INNER JOIN \"dims_table\" AS \"dims\" ON \"dims\".\"e5_id\" = \"e5\".\"id\" " +
        "WHERE (\"e1\".\"name\" = 'hello' OR \"e1\".\"nick_name\" = 'world') AND \"e2\".\"obj\" IS NOT NULL AND \"e3\".\"time\" < 9876543 " +
        "AND \"e3\".\"time\" >= 5324534 AND \"e1\".\"obj\"->>'name' = 'obj name' AND \"e2\".\"obj\"->>'integer' > 25 " +
        "AND \"e3\".\"obj\" = SqlBuilderTest.Obj(name=lin, integer=123, time=" + time + ") " +
        "ORDER BY \"e1\".\"time\" ASC, \"e3\".\"time\" DESC LIMIT 100 OFFSET 10");
    assertEquals(template.parameterizedSQL(),
      "SELECT \"e1\".\"name\" AS \"name\", \"e1\".\"obj\"->>'name' AS \"obj_name\", \"e2\".\"nick_name\" AS \"nick_name\", \"e3\".\"time\" AS \"time\", " +
        "\"e4\".\"name\" AS \"entity4_name\", \"e4\".\"time\" AS \"entity4_time\", \"e5\".\"id\" AS \"entity5_id\", \"e5\".\"e1_id\" AS \"entity5e1_id\", " +
        "\"e5\".\"name\" AS \"entity5_name\", \"e5\".\"time\" AS \"entity5_time\" FROM \"table_1\" AS \"e1\" " +
        "LEFT JOIN \"table_2\" AS \"e2\" ON \"e1\".\"e2_id\" = \"e2\".\"id\" " +
        "INNER JOIN \"table_3\" AS \"e3\" ON \"e1\".\"e3_id\" = \"e3\".\"id\" " +
        "RIGHT JOIN \"table_4\" AS \"e4\" ON \"e1\".\"id\" = \"e4\".\"e1_id\" " +
        "LEFT JOIN \"table_5\" AS \"e5\" ON \"e1\".\"id\" = \"e5\".\"e1_id\" " +
        "INNER JOIN \"dims_table\" AS \"dims\" ON \"dims\".\"e5_id\" = \"e5\".\"id\" " +
        "WHERE (\"e1\".\"name\" = #{$1} OR \"e1\".\"nick_name\" = #{$2}) AND \"e2\".\"obj\" IS NOT NULL AND \"e3\".\"time\" < #{$3} " +
        "AND \"e3\".\"time\" >= #{$4} AND \"e1\".\"obj\"->>'name' = #{$5} AND \"e2\".\"obj\"->>'integer' > #{$6} AND \"e3\".\"obj\" = #{$7, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$ObjectHandler} " +
        "ORDER BY \"e1\".\"time\" ASC, \"e3\".\"time\" DESC LIMIT #{$8} OFFSET #{$9}");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "hello");
    assertEquals(parameters.get("$2"), "world");
    assertEquals(parameters.get("$3"), 9876543);
    assertEquals(parameters.get("$4"), 5324534);
    assertEquals(parameters.get("$5"), "obj name");
    assertEquals(parameters.get("$6"), 25);
    assertEquals(parameters.get("$8"), 100);
    assertEquals(parameters.get("$9"), 10);
  }

  @Test
  public void test_SQL函数() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class, "e1")
      .where(sb.field("e1.name").startsWith("li"))
      .where(sb.field("e1.name").concat("i").eq("li"))
      .where(sb.field("e1.nickName").contains("foo"))
      .where(sb.field("e1.time").cast("bigint").eq(494329L))
      .where(sb.field("e1.time").between(10, 100))
      .where(sb.field("e1.rank").in(1, 2, 3, 4, 5, 6))
      .select(sb.field("e1.rank").onCase()
        .when(1).then("red")
        .when(2).then("blue")
        .when(3).then("green")
        .Else("black"), "color")
      .select(sb.id("1").count().over(), "count")
      .select(sb.rowNumber().over("ORDER BY e1.rank DESC"), "rowNum")
      .template();

    assertEquals(template.literalSQL(),
      "SELECT (CASE \"e1\".\"rank\" WHEN 1 THEN 'red' WHEN 2 THEN 'blue' WHEN 3 THEN 'green' ELSE 'black' END) AS \"color\", " +
        "COUNT(1) OVER() AS \"count\", ROW_NUMBER() OVER(ORDER BY e1.rank DESC) AS \"row_num\" FROM \"table_1\" AS \"e1\" " +
        "WHERE (\"e1\".\"name\" LIKE CONCAT('li', '%')) AND CONCAT(\"e1\".\"name\", 'i') = 'li' " +
        "AND (\"e1\".\"nick_name\" LIKE CONCAT('%', 'foo', '%')) AND CAST(\"e1\".\"time\" AS bigint) = 494329 " +
        "AND (\"e1\".\"time\" BETWEEN 10 AND 100) AND (\"e1\".\"rank\" IN(1, 2, 3, 4, 5, 6))");
    assertEquals(template.parameterizedSQL(),
      "SELECT (CASE \"e1\".\"rank\" WHEN #{$1} THEN #{$2} WHEN #{$3} THEN #{$4} WHEN #{$5} THEN #{$6} ELSE #{$7} END) AS \"color\", " +
        "COUNT(1) OVER() AS \"count\", " +
        "ROW_NUMBER() OVER(ORDER BY e1.rank DESC) AS \"row_num\" " +
        "FROM \"table_1\" AS \"e1\" WHERE (\"e1\".\"name\" LIKE CONCAT(#{$8}, '%')) " +
        "AND CONCAT(\"e1\".\"name\", #{$9}) = #{$10} " +
        "AND (\"e1\".\"nick_name\" LIKE CONCAT('%', #{$11}, '%')) " +
        "AND CAST(\"e1\".\"time\" AS bigint) = #{$12} " +
        "AND (\"e1\".\"time\" BETWEEN #{$13} AND #{$14}) " +
        "AND (\"e1\".\"rank\" IN(#{$15}, #{$16}, #{$17}, #{$18}, #{$19}, #{$20}))");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), 1);
    assertEquals(parameters.get("$2"), "red");
    assertEquals(parameters.get("$3"), 2);
    assertEquals(parameters.get("$4"), "blue");
    assertEquals(parameters.get("$5"), 3);
    assertEquals(parameters.get("$6"), "green");
    assertEquals(parameters.get("$7"), "black");
    assertEquals(parameters.get("$8"), "li");
    assertEquals(parameters.get("$9"), "i");
    assertEquals(parameters.get("$10"), "li");
    assertEquals(parameters.get("$11"), "foo");
    assertEquals(parameters.get("$12"), 494329L);
    assertEquals(parameters.get("$13"), 10);
    assertEquals(parameters.get("$14"), 100);
    assertEquals(parameters.get("$15"), 1);
    assertEquals(parameters.get("$16"), 2);
    assertEquals(parameters.get("$17"), 3);
    assertEquals(parameters.get("$18"), 4);
    assertEquals(parameters.get("$19"), 5);
    assertEquals(parameters.get("$20"), 6);
  }

  @Test
  public void test_SQL聚合函数() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class, "e1")
      .innerJoin(Entity2.class, "e2", sb.field("e1.e2Id").eq(sb.field("e2.id")))
      .groupBy("e1.name", "e2.name")
      .having(sb.field("e1.rank").sum().ge(1000))
      .select(sb.field("e2.name").distinct().count(), "count")
      .select(sb.field("e1.time").max(), "maxTime")
      .select(sb.field("e2.time").min(), "minTime")
      .select(sb.field("e1.rank").sum(), "sumRank")
      .select(sb.field("e1.rank").avg(), "avgRank")
      .template();

    assertEquals(template.literalSQL(),
      "SELECT COUNT(DISTINCT \"e2\".\"name\") AS \"count\", " +
        "MAX(\"e1\".\"time\") AS \"max_time\", " +
        "MIN(\"e2\".\"time\") AS \"min_time\", " +
        "SUM(\"e1\".\"rank\") AS \"sum_rank\", " +
        "AVG(\"e1\".\"rank\") AS \"avg_rank\" " +
        "FROM \"table_1\" AS \"e1\" INNER JOIN \"table_2\" AS \"e2\" ON \"e1\".\"e2_id\" = \"e2\".\"id\" " +
        "GROUP BY \"e1\".\"name\", \"e2\".\"name\" HAVING SUM(\"e1\".\"rank\") >= 1000");
  }

  @Test
  public void test_count() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class)
      .where(sb.field("name").eq("hello").or(sb.field("nickName").eq("world")))
      .where(sb.field("time").ge(5324534))
      .select(sb.id("1").count())
      .template();

    assertEquals(template.parameterizedSQL(),
      "SELECT COUNT(1) FROM \"table_1\" WHERE (\"name\" = #{$1} OR \"nick_name\" = #{$2}) AND \"time\" >= #{$3}");
  }

  @Test
  public void test_distinct() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class)
      .where(sb.field("name").eq("hello").or(sb.field("nickName").eq("world")))
      .where(sb.field("time").ge(5324534))
      .select(sb.field("name").distinct())
      .template();

    assertEquals(template.parameterizedSQL(),
      "SELECT DISTINCT \"name\" FROM \"table_1\" WHERE (\"name\" = #{$1} OR \"nick_name\" = #{$2}) AND \"time\" >= #{$3}");
  }

  @Test
  public void test_where子查询() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class, "e1")
      .where(sb.field("e1.rank").notIn(
        sb.from(Entity2.class)
          .where(sb.field("e1.e2Id").eq(sb.field("id")))
          .where(sb.field("time").max().ge(10000))
          .select("rank")
          .subQuery()
      ))
      .where(sb.field("e1.addresses").eq(Arrays.asList("beijing", "changsha")))
      .where(sb.field("e1.name").eq("li"))
      .where(sb.field("e1.time").lt(10900))
      .where(sb.field("e1.time").ge(
        sb.from(Entity3.class)
          .where(sb.field("name").eq("lo"))
          .select(sb.field("time").min())
          .subQuery()
      ))
      .where(sb.exists(
        sb.from(Entity2.class)
          .where(sb.field("name").eq("hello"))
          .select("name")
          .subQuery()
      ))
      .template();

    assertEquals(SqlFormatter.format(template.parameterizedSQL()),
      "\n" +
        "    SELECT\n" +
        "        * \n" +
        "    FROM\n" +
        "        \"table_1\" AS \"e1\" \n" +
        "    WHERE\n" +
        "        (\n" +
        "            \"e1\".\"rank\" NOT IN(\n" +
        "                SELECT\n" +
        "                    \"rank\" \n" +
        "                FROM\n" +
        "                    \"table_2\" \n" +
        "                WHERE\n" +
        "                    \"e1\".\"e2_id\" = \"id\" \n" +
        "                    AND MAX(\"time\") >= #{$1}\n" +
        "            )\n" +
        "        ) \n" +
        "        AND \"e1\".\"addresses\" = #{$4, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$AddressesHandler} \n" +
        "        AND \"e1\".\"name\" = #{$5} \n" +
        "        AND \"e1\".\"time\" < #{$6} \n" +
        "        AND \"e1\".\"time\" >= (\n" +
        "            SELECT\n" +
        "                MIN(\"time\") \n" +
        "            FROM\n" +
        "                \"table_3\" \n" +
        "            WHERE\n" +
        "                \"name\" = #{$2}\n" +
        "        ) \n" +
        "        AND EXISTS(\n" +
        "            SELECT\n" +
        "                \"name\" \n" +
        "            FROM\n" +
        "                \"table_2\" \n" +
        "            WHERE\n" +
        "                \"name\" = #{$3}\n" +
        "        )");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), 10000);
    assertEquals(parameters.get("$2"), "lo");
    assertEquals(parameters.get("$3"), "hello");
    assertEquals(parameters.get("$4"), Arrays.asList("beijing", "changsha"));
    assertEquals(parameters.get("$5"), "li");
    assertEquals(parameters.get("$6"), 10900);
  }

  @Test
  public void test_from子查询() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(
      sb.from(Entity1.class, "e1")
        .leftJoin(Entity2.class, "e2", sb.field("e1.e2Id").eq(sb.field("e2.id")))
        .where(sb.field("e1.time").ge(10000))
        .where(sb.field("e1.name").contains("li"))
        .where(sb.field("e1.rank").lt(10000))
        .where(sb.field("e2.name").startsWith("lo"))
        .where(sb.field("e2.rank").in(1, 2, 3))
        .where(sb.field("e2.time").in(
          sb.from(Entity5.class).select("time").subQuery()
        ))
        .limit(1, 101)
        .select(sb.rowNumber().over("ORDER BY e1.time"), "rowNum")
        .select("e1.name", "e1.nickName")
        .subQuery(), "ef")
      .innerJoin(
        sb.from(Entity3.class, "e3")
          .leftJoin(Entity4.class, "e4", sb.field("e3.id").eq(sb.field("e4.id")))
          .where(sb.field("ef.name").endsWith("lin"))
          .select("e3.rank", "e4.time")
          .subQuery(), "ej")
      .select("ef.*", "ej.rank", "ej.time")
      .template();

    assertEquals(SqlFormatter.format(template.parameterizedSQL()),
      "\n" +
        "    SELECT\n" +
        "        \"ef\".*,\n" +
        "        \"ej\".\"rank\" AS \"rank\",\n" +
        "        \"ej\".\"time\" AS \"time\" \n" +
        "    FROM\n" +
        "        (SELECT\n" +
        "            ROW_NUMBER() OVER(\n" +
        "        ORDER BY\n" +
        "            e1.time) AS \"row_num\",\n" +
        "            \"e1\".\"name\" AS \"name\",\n" +
        "            \"e1\".\"nick_name\" AS \"nick_name\" \n" +
        "        FROM\n" +
        "            \"table_1\" AS \"e1\" \n" +
        "        LEFT JOIN\n" +
        "            \"table_2\" AS \"e2\" \n" +
        "                ON \"e1\".\"e2_id\" = \"e2\".\"id\" \n" +
        "        WHERE\n" +
        "            \"e1\".\"time\" >= #{$1} \n" +
        "            AND (\n" +
        "                \"e1\".\"name\" LIKE CONCAT('%', #{$2}, '%')\n" +
        "            ) \n" +
        "            AND \"e1\".\"rank\" < #{$3} \n" +
        "            AND (\n" +
        "                \"e2\".\"name\" LIKE CONCAT(#{$4}, '%')\n" +
        "            ) \n" +
        "            AND (\n" +
        "                \"e2\".\"rank\" IN(\n" +
        "                    #{$5}, #{$6}, #{$7}\n" +
        "                )\n" +
        "            ) \n" +
        "            AND (\n" +
        "                \"e2\".\"time\" IN(\n" +
        "                    SELECT\n" +
        "                        \"time\" \n" +
        "                    FROM\n" +
        "                        \"table_5\"\n" +
        "                )\n" +
        "            ) LIMIT #{$8} OFFSET #{$9}\n" +
        "        ) AS \"ef\" \n" +
        "    INNER JOIN\n" +
        "        LATERAL(SELECT\n" +
        "            \"e3\".\"rank\" AS \"rank\",\n" +
        "            \"e4\".\"time\" AS \"time\" \n" +
        "        FROM\n" +
        "            \"table_3\" AS \"e3\" \n" +
        "        LEFT JOIN\n" +
        "            \"table_4\" AS \"e4\" \n" +
        "                ON \"e3\".\"id\" = \"e4\".\"id\" \n" +
        "        WHERE\n" +
        "            (\"ef\".\"name\" LIKE CONCAT('%', #{$10}))) AS \"ej\" \n" +
        "            ON TRUE");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), 10000);
    assertEquals(parameters.get("$2"), "li");
    assertEquals(parameters.get("$3"), 10000);
    assertEquals(parameters.get("$4"), "lo");
    assertEquals(parameters.get("$5"), 1);
    assertEquals(parameters.get("$6"), 2);
    assertEquals(parameters.get("$7"), 3);
    assertEquals(parameters.get("$8"), 101);
    assertEquals(parameters.get("$9"), 1);
    assertEquals(parameters.get("$10"), "lin");
  }

  @Test
  public void test_Insert() {
    long time = System.currentTimeMillis();
    Obj obj = new Obj("lin", 123, time);

    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class)
      .insert()
      .set("name", "li")
      .set("nickName", "lin")
      .set("rank", 1)
      .set("time", time)
      .set("obj", obj)
      .ending()
      .set("name", "li2")
      .set("nickName", "lin2")
      .set("rank", 2)
      .set("time", time)
      .set("obj", obj)
      .template();

    assertEquals(template.parameterizedSQL(),
      "INSERT INTO \"table_1\" (\"name\", \"nick_name\", \"rank\", \"time\", \"obj\") VALUES " +
        "(#{$1}, #{$2}, #{$3}, #{$4}, #{$5, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$ObjectHandler}), " +
        "(#{$6}, #{$7}, #{$8}, #{$9}, #{$10, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$ObjectHandler})");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "li");
    assertEquals(parameters.get("$2"), "lin");
    assertEquals(parameters.get("$3"), 1);
    assertEquals(parameters.get("$4"), time);
    assertEquals(parameters.get("$6"), "li2");
    assertEquals(parameters.get("$7"), "lin2");
    assertEquals(parameters.get("$8"), 2);
    assertEquals(parameters.get("$9"), time);
  }

  @Test
  public void test_InsertFrom() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity5.class)
      .insert(sb.from(Entity1.class)
        .select(sb.field("id"), "e1Id")
        .select("name", "time")
        .subQuery())
      .template();

    assertEquals(template.parameterizedSQL(),
      "INSERT INTO \"table_5\" (SELECT \"id\" AS \"e1_id\", \"name\", \"time\" FROM \"table_1\")");
  }

  @Test
  public void test_单表更新() {
    long time = System.currentTimeMillis();
    Obj obj = new Obj("lin", 123, time);

    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class).update()
      .set("name", "foo")
      .set("nickName", "bar")
      .set("time", 1322434L)
      .set("obj", obj)
      .template();
    assertEquals(template.parameterizedSQL(),
      "UPDATE \"table_1\" SET \"name\" = #{$1}, \"nick_name\" = #{$2}, \"time\" = #{$3}, \"obj\" = #{$4, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$ObjectHandler}");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "foo");
    assertEquals(parameters.get("$2"), "bar");
    assertEquals(parameters.get("$3"), 1322434L);
  }

  @Test
  public void test_连接表更新() {
    long time = System.currentTimeMillis();
    Obj obj = new Obj("lin", 123, time);

    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class, "e1")
      .innerJoin(Entity2.class, "e2", sb.field("e1.e2Id").eq(sb.field("e2.id")))
      .where(sb.field("e1.name").eq("foo"))
      .where(sb.field("e2.name").eq("bar"))
      .where(sb.field("e2.obj").eq(obj))
      .update()
      .set("e1.name", "lin")
      .set("e1.nickName", "linSH")
      .set("e1.obj", obj)
      .template();
    assertEquals(template.parameterizedSQL(),
      "UPDATE \"table_1\" AS \"e1\" INNER JOIN \"table_2\" AS \"e2\" ON \"e1\".\"e2_id\" = \"e2\".\"id\" " +
        "SET \"e1\".\"name\" = #{$1}, \"e1\".\"nick_name\" = #{$2}, \"e1\".\"obj\" = #{$3, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$ObjectHandler} " +
        "WHERE \"e1\".\"name\" = #{$4} AND \"e2\".\"name\" = #{$5} AND \"e2\".\"obj\" = #{$6, typeHandler=com.github.sun.foundation.sql.SqlBuilderTest$ObjectHandler}");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "lin");
    assertEquals(parameters.get("$2"), "linSH");
    assertEquals(parameters.get("$4"), "foo");
    assertEquals(parameters.get("$5"), "bar");
  }

  @Test
  public void test_单表删除() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class)
      .where(sb.field("name").eq("hello").or(sb.field("nickName").eq("world")))
      .where(sb.field("rank").isNotNull())
      .delete()
      .template();
    assertEquals(template.parameterizedSQL(),
      "DELETE FROM \"table_1\" WHERE (\"name\" = #{$1} OR \"nick_name\" = #{$2}) AND \"rank\" IS NOT NULL");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "hello");
    assertEquals(parameters.get("$2"), "world");
  }

  @Test
  public void test_连接表删除() {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Entity1.class, "e1")
      .innerJoin(Entity2.class, "e2", sb.field("e1.e2Id").eq(sb.field("e2.id")))
      .where(sb.field("e1.name").eq("foo"))
      .where(sb.field("e2.name").eq("bar"))
      .delete()
      .template();
    assertEquals(template.parameterizedSQL(),
      "DELETE \"e1\", \"e2\" FROM \"table_1\" AS \"e1\" INNER JOIN \"table_2\" AS \"e2\" ON \"e1\".\"e2_id\" = \"e2\".\"id\" WHERE \"e1\".\"name\" = #{$1} AND \"e2\".\"name\" = #{$2}");

    Map<String, Object> parameters = template.parametersAsMap();
    assertEquals(parameters.get("$1"), "foo");
    assertEquals(parameters.get("$2"), "bar");
  }
}

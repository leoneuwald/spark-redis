package com.redislabs.provider.redis.df

import com.redislabs.provider.redis.df.Person.{data, _}
import com.redislabs.provider.redis.rdd.RedisStandaloneSuite
import org.apache.spark.sql.redis._
import org.apache.spark.sql.types._
import org.scalatest.Matchers

import scala.collection.JavaConverters._

/**
  * TODO: test more schema data types
  *
  * @author The Viet Nguyen
  */
class HashDataframeStandaloneSuite extends RedisStandaloneSuite with Matchers {

  import TestSqlImplicits._

  test("save and load dataframe by default") {
    val tableName = generateTableName(TableNamePrefix)
    val df = spark.createDataFrame(data)
    df.write.format(RedisFormat).
      option(SqlOptionTableName, tableName)
      .save()
    val loadedDf = spark.read.format(RedisFormat)
      .option(SqlOptionTableName, tableName)
      .load()
      .cache()
    loadedDf.show()
    loadedDf.count() shouldBe df.count()
    loadedDf.schema shouldBe df.schema
    val loadedArr = loadedDf.as[Person].collect()
    loadedArr.sortBy(_.name) shouldBe data.toArray.sortBy(_.name)
  }

  test("save and load dataframe with hash mode") {
    val tableName = generateTableName(TableNamePrefix)
    val df = spark.createDataFrame(data)
    df.write.format(RedisFormat)
      .option(SqlOptionModel, SqlOptionModelHash)
      .option(SqlOptionTableName, tableName)
      .save()
    val loadedDf = spark.read.format(RedisFormat)
      .option(SqlOptionModel, SqlOptionModelHash)
      .option(SqlOptionTableName, tableName)
      .load()
      .cache()
    loadedDf.show()
    loadedDf.count() shouldBe df.count()
    loadedDf.schema shouldBe df.schema
    val loadedArr = loadedDf.as[Person].collect()
    loadedArr.sortBy(_.name) shouldBe data.toArray.sortBy(_.name)
  }

  test("save with hash mode and load dataframe") {
    val tableName = generateTableName(TableNamePrefix)
    val df = spark.createDataFrame(data)
    df.write.format(RedisFormat)
      .option(SqlOptionModel, SqlOptionModelHash)
      .option(SqlOptionTableName, tableName)
      .save()
    val loadedDf = spark.read.format(RedisFormat)
      .option(SqlOptionTableName, tableName)
      .load()
      .cache()
    loadedDf.show()
    loadedDf.count() shouldBe df.count()
    loadedDf.schema shouldBe df.schema
    val loadedArr = loadedDf.as[Person].collect()
    loadedArr.sortBy(_.name) shouldBe data.toArray.sortBy(_.name)
  }

  test("save and load with hash mode dataframe") {
    val tableName = generateTableName(TableNamePrefix)
    val df = spark.createDataFrame(data)
    df.write.format(RedisFormat).option(SqlOptionTableName, tableName).save()
    val loadedDf = spark.read.format(RedisFormat)
      .option(SqlOptionModel, SqlOptionModelHash)
      .option(SqlOptionTableName, tableName)
      .load()
      .cache()
    loadedDf.show()
    loadedDf.count() shouldBe df.count()
    loadedDf.schema shouldBe df.schema
    val loadedArr = loadedDf.as[Person].collect()
    loadedArr.sortBy(_.name) shouldBe data.toArray.sortBy(_.name)
  }

  test("load dataframe with inferred schema") {
    val tableName = generateTableName(TableNamePrefix)
    val node = redisConfig.initialHost
    val conn = node.connect()
    val data = Seq(
      Map("name" -> "John", "age" -> "30", "address" -> "60 Wall Street", "salary" -> "150.5"),
      Map("name" -> "Peter", "age" -> "35", "address" -> "110 Wall Street", "salary" -> "200.3")
    )
    data.map(_.asJava)
      .foreach { person =>
        conn.hmset(tableName + ":" + person.get("name"), person)
      }
    val loadedDf = spark.read.format(RedisFormat)
      .option(SqlOptionKeysPattern, tableName + ":*")
      .option(SqlOptionInferSchema, "true")
      .load()
      .cache()
    loadedDf.show()
    loadedDf.count() shouldBe 2
    val loadedArr = loadedDf
      .collect()
      .map { row =>
        val name = row.getAs[String]("name")
        val age = row.getAs[String]("age").toInt
        val address = row.getAs[String]("address")
        val salary = row.getAs[String]("salary").toDouble
        Person(name, age, address, salary)
      }
    loadedArr.sortBy(_.name) shouldBe Person.data.toArray.sortBy(_.name)
  }

  test("load dataframe with provided schema") {
    val tableName = generateTableName(TableNamePrefix)
    val node = redisConfig.initialHost
    val conn = node.connect()
    val data = Seq(
      Map("name" -> "John", "age" -> "30", "address" -> "60 Wall Street", "salary" -> "150.5"),
      Map("name" -> "Peter", "age" -> "35", "address" -> "110 Wall Street", "salary" -> "200.3")
    )
    data.map(_.asJava)
      .foreach { person =>
        conn.hmset(tableName + ":" + person.get("name"), person)
      }
    val loadedDf = spark.read.format(RedisFormat)
      .option(SqlOptionKeysPattern, tableName + ":*")
      .schema(StructType(Array(
        StructField("name", StringType),
        StructField("age", IntegerType),
        StructField("address", StringType),
        StructField("salary", DoubleType)
      )))
      .load()
      .cache()
    loadedDf.show()
    loadedDf.count() shouldBe 2
    val loadedArr = loadedDf.as[Person].collect()
    loadedArr.sortBy(_.name) shouldBe Person.data.toArray.sortBy(_.name)
  }
}
# LeanCloud Scala SDK

[![Build Status](https://travis-ci.org/aiyanbo/leancloud-scala-sdk.svg?branch=master)](https://travis-ci.org/aiyanbo/leancloud-scala-sdk)

## SBT

```scala

libraryDependencies += "org.jmotor.leancloud" %% "leancloud-scala-sdk" % "1.0.0-SNAPSHOT"

```

## Dependencies

- Typesafe-Config: [![Current Version](http://stack-badges.herokuapp.com/maven-central/com.typesafe/config/current.svg)](http://stack-badges.herokuapp.com/maven-central/com.typesafe/config)
- AsyncHttpClient: [![Current Version](http://stack-badges.herokuapp.com/maven-central/com.ning/async-http-client/current.svg)](http://stack-badges.herokuapp.com/maven-central/com.ning/async-http-client)

## Getting Started

```scala

import org.jmotor.leancloud.LeanCloudClient._
import org.jmotor.conversions.JsonConversions._

object Users {
  private implicit val className = "users"

  def add(username: String, age: Integer) = {
    insert(Map("username" -> username, "age" -> age))
  }

  def getUser(objectId: String) = {
    get(objectId)
  }

  def del(objectId: String) = {
    delete(objectId)
  }

  def changeAge(username: String, age: Integer) = {
    update(Map("username" -> username), Map("age" -> age))
  }

  def listUsersByAge(age: Integer) = {
    query(Map("age" -> Map("$lt" -> age)))
  }

}

```

## Others

```scala

batch

exists

```

## Requirement

- JDK 1.8+
- Scala 2.11
# OSQL
## Presentation
This is an intermediate language to generate the SQL code structuring the database so as to implement the concepts of object-oriented programming including inheritance, composition, aggregation, metadata (annotations), ... while maintaining native language concepts such as events, insertions, routines ...

## How it works
A database structured using this language is passed to the analyser to create objects representing the defined entities. These objects are open to changes and are easy to handle before generating the final sql database structure.

## OSQL vs ORM
We can consider this solution as an alternative to creating the database using a code first ORM. 
The main differences are:
 * Involved Layer: osql depends on the database technology in opposition to the ORM which depends on client application language.
 * Database Structure: ORM helps developers consider the database as it is based on objects. OSQL helps them creating such a database.
 * Direct Access: As it is sql injected, OSQL, unlike ORM, let developers access to all the sql native features as events, procedures, priviledges, ...

## Why using OSQL
The benefit sought in this case is to make the database accessible to queries third customers for scalability purposes without degrading performance (sending an optimized SQL query instead of sending several requests pending whenever the answer makes save time and trafficking). Therefore an important part of the logic (which defines if the database is coherent) is transferred to the data server.

# Use
## Download
The released versions are available on [osql.sf.net](https://sourceforge.net/projects/osql/) repository or as a maven dependency on [mymavenrepo.com](https://mymavenrepo.com/repo/0qo9dAdBcLRywnctciNm/)

## Run on command line
```shell
java -jar osql.jar -i base.osql -m hard -d mysql | mysql -uuser -ppass
```

## Integrate into your maven project
```XML
  <repositories>
    <repository>
      <id>myMavenRepo.read</id>
      <url>https://mymavenrepo.com/repo/0qo9dAdBcLRywnctciNm/</url>
    </repository>
    ...
  </repositories>
  <dependencies>
    <dependency>
      <groupId>net.sf.osql</groupId>
      <artifactId>osql</artifactId>
      <version>1.1</version>
      <scope>compile</scope>
    </dependency>
    ...
  </dependencies>
```

# Examples
## Inheritence
```
class A {
	int a_inherited;
	int a_local*;
	into (a_inherited, a_local) insert (1,2), (3,4);
}
class B from A{
	int b;
	into (a_inherited, a_local, b) insert (5,6,7);
}
```
## Agregation
```
class A {
	int a;
	into (a) insert 
	$objA = (100), $otherA = (200);
}
class B {
	A refToA key; //a reference must be a key
	into (refToA) insert ($otherA);
}
```
## Composition
```
class B {
	ref A refToA key;
	into (refToA) insert $objB = ($objA);
}	
ref class A {
	int a;
	into (a, $ref, $rid) insert 
	$objA = (100, ‘B.refToA’, $objB);
}
```
## Metadata
```
@interface output { }
@interface age { int max_age; }
@age(max_age=15) class Child {}
class Boy from Child {}
@age(max_age=50) class Man {}
@output()
class Ages uses age {
	varchar(64) name unique;
	into (name) insert (@age);
}
```
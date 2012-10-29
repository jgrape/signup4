SignUp
======

General
-------

This is the fourth version of the SignUp Service.

 - It is written in Scala and based on the Play Framework.
 - Anorm is used for SQL Database access*
 - Heroku is used for deployment

*) H2 when running locally, Postgres on Heroku


### Play Framework ###

 - Play 2.0 - http://www.playframework.org/
 - Scala 2.9.1 (part of Play) - http://www.scala-lang.org/
 - Anorm (part of play) - http://www.playframework.org/documentation/2.0/ScalaAnorm

### Presentation ###

 - Twitter Bootstrap 2.0


License, credits and stuff
--------------------------

The module securesocial is written by Jorge Aliss (jaliss at gmail dot com, twitter: @jaliss) and
is licensed under the Apache License, Version 2.0. You'll find his project at https://github.com/jaliss/securesocial.

The rest of the code is (c) by Mats Strandberg and Jan Grape and is
licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Setting Up Development Environment
----------------------------------

### Basics ###

In order to set up the development environment you need to:

 - Clone this project from GitHub
 - Install Play 2.0
 - Install Heroku Toolkit - https://toolbelt.heroku.com/ (Optional, only needed to deploy to Heroku)
 - ```play eclipsify``` or ```play idea```

### "Persistent Database" - Postgres ###

When running SignUp as described above, the H2 database is used.
The H2 database is running in-memory, in-process. This is handy,
because of the simple setup, but each time you stop Play your database
will be gone. It would be nice to have a persistent database,
e.g. a disk based database. When running on Heroku, Postgres will be used.
For these reasons, a local installation of Postgres is good.

To install Postgres on MacOS X:

 - Download Postgres from [postgres.org](http://www.postgresql.org/)
 - Install a standard Postgres (You may have to restart your Mac as the installation fiddles with shared memory)
 - Set the password for the DATABASE (super)user postgres when prompted
 - Finish the installation
 - On MacOS X: A UNIX user is created: 'PostgreSQL'
   This user seem to get some password that you cannot find out.
   Reset the password of the UNIX user 'PostgreSQL' to something you know, and something safe, as this is a real MacOS X user.
 - Do some stuff from the command prompt to create a database and a database user

```
$ su - PostgreSQL
Password: <Enter the password of your UNIX user 'PostgreSQL'>
$ createdb signup
Password: <Enter the password of your DATABASE user 'postgres'>
$ psql -s signup
Password: <Enter the password of your DATABASE user 'postgres'>
signup=# create user signup4 password 's7p2+';
signup=# grant all privileges on database signup to signup4;
```

Now you can start SignUp with Postgres using

./playrunpostgres.sh

Run SignUp
----------

Once you have the development environment set up you should be able to do

```play run```

And the direct your browser to
[http://localhost:9000](http://localhost:9000)

If you have a local Postgres (installed as described above) you may start SignUp using

./playrunpostgres.sh

Deploy
------

Deployment is done to Heroku.

It runs at [http://signup4.herokuapp.com](http://signup4.herokuapp.com)

To be able to deploy to Heroku you must:

* Install Heroku toolbelt.
* Do a 'heroku login'.
* cd into the root of this application
* git remote add heroku git@heroku.com:signup4.git
* git push heroku master

I (Mats) had to do a 'heroku keys:add ~/.ssh/id_rsa.pub' on one of my
machines as I first got 'Permission denied (publickey).' when trying
to 'git push heroku master'

Access Postgres on Heroku
-------------------------

Install the [SQL console plugin](https://github.com/ddollar/heroku-sql-console) in Heroku:

```heroku plugins:install git://github.com/ddollar/heroku-sql-console.git```

Run the SQL console in Heroku:
```
pyttemackan:signup janne$ heroku sql
SQL console for signup4.heroku.com
SQL> show tables
+-----------------+
|   table_name    |
+-----------------+
| events          |
| participations  |
| play_evolutions |
| users           |
+-----------------+
SQL> select * from users
+--------------------------------------------------------------------------------+
| id | first_name | last_name |        comment        |      email       | phone |
+--------------------------------------------------------------------------------+
| -2 | Torbjörn   | Fälldin   |                       | unknown@crisp.se |       |
| -3 | Göran      | Persson   | En f.d. statsminister | unknown@crisp.se |       |
| -4 | Frodo      | Baggins   | Ringbärare            | unknown@crisp.se |       |
| -1 | Fredrik    | Unknown   | En glad statsminister | unknown@crisp.se |       |
+--------------------------------------------------------------------------------+
SQL>
```

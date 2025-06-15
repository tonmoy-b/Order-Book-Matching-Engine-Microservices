   print('Giving user:admin readWrite privileges on dbase01...');
   db = db.getSiblingDB('admin'); // Switch to the admin database
   //db.grantRolesToUser("admin", [{ role: "readWrite", db: "dbase01" }]);
   //db.grantRolesToUser("admin", [{ role: "dbAdmin", db: "dbase01" }]);
   print('User privileges granted.');

   //db = db.getSiblingDB('dbase01');
//   db.createUser({
//       user:"admin",
//       pwd:"admin",
//       roles: [
//          { role: "dbAdmin", db: "dbase01" }
//       ]
//   });
   //db.grantRolesToUser("admin", [{ role: "dbAdmin", db: "dbase01" }]);
   //db.createCollection("coll01");
   //db.coll01.insertOne({"remark": "first"});
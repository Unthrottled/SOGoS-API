import {HMAC_KEY, mongoUrl} from "./ENV";
import {MongoClient} from "mongodb";
import {EffectSchema, UserSchema} from "../../nextGen/src/memory/Schemas";
import crypto from "crypto";
import Chalk from 'chalk';

export const hashString = (value: string) =>
  crypto.createHmac('SHA256', HMAC_KEY)
    .update(value).digest('hex');

// todo: rxjs to enable known completion of updates
new Promise((resolve, reject) =>
  MongoClient.connect(mongoUrl, {useUnifiedTopology: true, useNewUrlParser: true}, ((error, result) => {
  if (!error) {
    const db = result.db('sogos');
    const userCollection = db.collection(UserSchema.COLLECTION);
    const effectCollection = db.collection(EffectSchema.COLLECTION);
    const cursor = userCollection.find({}).stream();
    cursor.on('data', doc => {
      console.log(doc);
      // effectCollection.findOne({
      //   [EffectSchema.GLOBAL_USER_IDENTIFIER]: doc[UserSchema.GLOBAL_USER_IDENTIFIER],
      //   [EffectSchema.NAME]: 'USER_CREATED',
      // }, ((error1, result1) => {
      //   if(!error1){
      //     const email = result1.content.email;
      //     const identifiers = hashString(email);
      //     console.log(`Adding ${identifiers} to email ${email}`);
      //     // danger zone!
      //     // userCollection.updateOne({
      //     //   _id: doc._id
      //     // }, {
      //     //   $push: { identifiers: identifiers}
      //     // }).then(()=> {
      //     //   console.log(`Wrote ${identifiers} to email ${email}`);
      //     // })
      //   }
      // }))
    });
    cursor.on('end', ()=>{
      result.close(()=>resolve())
    })
  }
}))
).then(()=>{
  console.info(Chalk.green('Something something something, complete.'))
});


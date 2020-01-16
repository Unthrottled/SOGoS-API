import {HMAC_KEY, mongoUrl} from "./ENV";
import {MongoClient} from "mongodb";
import {EffectSchema} from "../../nextGen/src/memory/Schemas";
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
    // const userCollection = db.collection(UserSchema.COLLECTION);
    // userCollection.deleteOne({
    //   _id: '5e2044b67c3f625b4d7036a1'
    // }).then(thing => {
    //   console.log(thing);
    //   result.close(resolve)
    // })
    // const cursor = db.collection(EffectSchema.COLLECTION)
    //   .deleteMany({[EffectSchema.GLOBAL_USER_IDENTIFIER]: '08d0cfda-b3bd-40e8-9e24-9eabedf28f1d'});
    // cursor.then(del => {
    //   console.log(del);
    //   result.close(resolve);
    // })
  }
  }))
).then(()=>{
  console.info(Chalk.green('Something something something, complete.'))
});


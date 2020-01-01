import {HMAC_KEY, mongoUrl} from "./ENV";
import {MongoClient} from "mongodb";
import {EffectSchema, UserSchema} from "../../nextGen/src/memory/Schemas";
import crypto from "crypto";


export const hashString = (value: string) =>
  crypto.createHmac('SHA256', HMAC_KEY)
    .update(value).digest('hex');

new Promise((resolve, reject) =>
  MongoClient.connect(mongoUrl, {useNewUrlParser: true}, ((error, result) => {
  if (!error) {
    const db = result.db('DEFAULT_DB');
    const userCollection = db.collection(UserSchema.COLLECTION);
    const effectCollection = db.collection(EffectSchema.COLLECTION);
    const cursor = userCollection.find({}).stream();
    cursor.on('data', doc => {
      console.log(doc);
      effectCollection.findOne({
        [EffectSchema.GLOBAL_USER_IDENTIFIER]: doc[UserSchema.GLOBAL_USER_IDENTIFIER],
        [EffectSchema.NAME]: 'USER_CREATED',
      }, ((error1, result1) => {
        if(!error1){
          // userCollection.updateOne({
          //   _id: doc._id
          // }, {
          //   $push: { identifiers: hashString(result1.content.email)}
          // })
        }
      }))
    });

  }
}))
)


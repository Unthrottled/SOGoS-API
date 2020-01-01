import {Router} from 'express';

const authenticatedRoutes = Router();

authenticatedRoutes.get('/user', ((req, res) => {
  // @ts-ignore
  console.log(req.claims);
  res.send();
}));

export default authenticatedRoutes;

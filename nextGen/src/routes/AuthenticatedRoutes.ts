import {Router} from 'express';
import historyRouter from '../api/History';
import {userHandler} from '../api/User';

const authenticatedRoutes = Router();

authenticatedRoutes.get('/user', userHandler);
authenticatedRoutes.use('/history', historyRouter);

export default authenticatedRoutes;

import {Router} from 'express';
import historyRouter from '../api/HistoryRoutes';
import {userHandler} from '../api/UserRoutes';

const authenticatedRoutes = Router();

authenticatedRoutes.get('/user', userHandler);
authenticatedRoutes.use('/history', historyRouter);

export default authenticatedRoutes;

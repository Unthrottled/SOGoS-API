import {Router} from 'express';
import activityRoutes from '../api/Activity';
import strategyRoutes from '../api/Strategy';

const authorizedRoutes = Router();

authorizedRoutes.use('/activity', activityRoutes);
authorizedRoutes.use('/strategy', strategyRoutes);

export default authorizedRoutes;

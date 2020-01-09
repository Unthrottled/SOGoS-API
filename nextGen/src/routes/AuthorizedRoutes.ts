import {Router} from 'express';
import activityRoutes from '../api/ActivityRoutes';
import strategyRoutes from '../api/Strategy';
import tacticalRoutes from '../api/Tactical';

const authorizedRoutes = Router();

authorizedRoutes.use('/activity', activityRoutes);
authorizedRoutes.use('/strategy', strategyRoutes);
authorizedRoutes.use('/tactical', tacticalRoutes);

export default authorizedRoutes;

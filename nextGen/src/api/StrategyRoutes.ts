import {Router} from 'express';
import objectivesRoutes from './ObjectiveRoutes';

const strategyRoutes = Router();

strategyRoutes.use('/objectives', objectivesRoutes);

export default strategyRoutes;

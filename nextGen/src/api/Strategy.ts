import {Router} from 'express';
import objectivesRoutes from './Objective';

const strategyRoutes = Router();

strategyRoutes.use('/objectives', objectivesRoutes);

export default strategyRoutes;

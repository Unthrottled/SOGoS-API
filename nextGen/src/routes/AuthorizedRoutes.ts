import {Router} from 'express';
import activityRoutes from "../api/Activity";

const authorizedRoutes = Router();

authorizedRoutes.use('/activity', activityRoutes);

export default authorizedRoutes;

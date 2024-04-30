import React from 'react';
import { Route } from 'react-router-dom';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import RedditSubsCategorized from './reddit-subs-categorized';
import RedditSubsCategorizedDetail from './reddit-subs-categorized-detail';
import RedditSubsCategorizedUpdate from './reddit-subs-categorized-update';
import RedditSubsCategorizedDeleteDialog from './reddit-subs-categorized-delete-dialog';

const RedditSubsCategorizedRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<RedditSubsCategorized />} />
    <Route path="new" element={<RedditSubsCategorizedUpdate />} />
    <Route path=":id">
      <Route index element={<RedditSubsCategorizedDetail />} />
      <Route path="edit" element={<RedditSubsCategorizedUpdate />} />
      <Route path="delete" element={<RedditSubsCategorizedDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default RedditSubsCategorizedRoutes;

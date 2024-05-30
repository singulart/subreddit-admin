import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Row, Col } from 'reactstrap';
import {} from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './reddit-subs-categorized.reducer';

export const RedditSubsCategorizedDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();
  const authenticated = useAppSelector(state => state.authentication.isAuthenticated);

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const redditSubsCategorizedEntity = useAppSelector(state => state.redditSubsCategorized.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="redditSubsCategorizedDetailsHeading">Subreddit</h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">ID</span>
          </dt>
          <dd>{redditSubsCategorizedEntity.id}</dd>
          <dt>
            <span id="sub">Subreddit</span>
          </dt>
          <dd>{redditSubsCategorizedEntity.sub}</dd>
          <dt>
            <span id="cat">Category</span>
          </dt>
          <dd>{redditSubsCategorizedEntity.cat}</dd>
          <dt>
            <span id="subcat">Subcategory</span>
          </dt>
          <dd>{redditSubsCategorizedEntity.subcat}</dd>
          <dt>
            <span id="niche">Niche</span>
          </dt>
          <dd>{redditSubsCategorizedEntity.niche}</dd>
        </dl>
        <Button tag={Link} to="/reddit-subs-categorized" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" /> <span className="d-none d-md-inline">Back</span>
        </Button>
        &nbsp;
        {authenticated ? (
          <Button tag={Link} to={`/reddit-subs-categorized/${redditSubsCategorizedEntity.id}/edit`} replace color="primary">
            <FontAwesomeIcon icon="pencil-alt" /> <span className="d-none d-md-inline">Edit</span>
          </Button>
        ) : (
          ''
        )}
      </Col>
    </Row>
  );
};

export default RedditSubsCategorizedDetail;

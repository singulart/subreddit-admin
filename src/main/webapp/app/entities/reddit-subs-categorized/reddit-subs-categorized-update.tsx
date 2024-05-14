import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Row, Col, FormText } from 'reactstrap';
import { isNumber, ValidatedField, ValidatedForm } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { mapIdList } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { IRedditSubsCategorized } from 'app/shared/model/reddit-subs-categorized.model';
import { getEntity, updateEntity, createEntity, reset } from './reddit-subs-categorized.reducer';

export const RedditSubsCategorizedUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const redditSubsCategorizedEntity = useAppSelector(state => state.redditSubsCategorized.entity);
  const loading = useAppSelector(state => state.redditSubsCategorized.loading);
  const updating = useAppSelector(state => state.redditSubsCategorized.updating);
  const updateSuccess = useAppSelector(state => state.redditSubsCategorized.updateSuccess);

  const handleClose = () => {
    navigate('/reddit-subs-categorized');
  };

  useEffect(() => {
    if (!isNew) {
      dispatch(getEntity(id));
    }
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  // eslint-disable-next-line complexity
  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }

    const entity = {
      ...redditSubsCategorizedEntity,
      ...values,
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          ...redditSubsCategorizedEntity,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="subredditsAdminApp.redditSubsCategorized.home.createOrEditLabel" data-cy="RedditSubsCategorizedCreateUpdateHeading">
            Create or edit a Subreddit
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew ? (
                <ValidatedField name="id" required readOnly id="reddit-subs-categorized-id" label="ID" validate={{ required: true }} />
              ) : null}
              <ValidatedField
                label="Subreddit"
                id="reddit-subs-categorized-sub"
                name="sub"
                data-cy="sub"
                type="text"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                  maxLength: { value: 64, message: 'This field cannot be longer than 64 characters.' },
                }}
              />
              <ValidatedField
                label="Category"
                id="reddit-subs-categorized-cat"
                name="cat"
                data-cy="cat"
                type="text"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                  maxLength: { value: 64, message: 'This field cannot be longer than 64 characters.' },
                }}
              />
              <ValidatedField
                label="Subcategory"
                id="reddit-subs-categorized-subcat"
                name="subcat"
                data-cy="subcat"
                type="text"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                  maxLength: { value: 64, message: 'This field cannot be longer than 64 characters.' },
                }}
              />
              <ValidatedField
                label="Niche"
                id="reddit-subs-categorized-niche"
                name="niche"
                data-cy="niche"
                type="text"
                validate={{
                  required: { value: true, message: 'This field is required.' },
                  maxLength: { value: 64, message: 'This field cannot be longer than 64 characters.' },
                }}
              />
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/reddit-subs-categorized" replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">Back</span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp; Save
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default RedditSubsCategorizedUpdate;

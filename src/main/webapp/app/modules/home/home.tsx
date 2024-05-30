import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';

import { Row, Col, Alert } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  return (
    <Row>
      <Col md="3" className="pad">
        <span className="hipster rounded" />
      </Col>
      <Col md="9">
        <h1 className="display-4">Welcome to the most comprehensive Reddit subreddits categorization project to date!</h1>
        <p className="lead">
          <a href="/reddit-subs-categorized">170k subs</a> are assigned three categories:{' '}
        </p>
        <p className="lead">
          <ul>
            <li>A top level, most generic one</li>
            <li>A more specific one</li>
            <li>Niche: the most specific one</li>
          </ul>
        </p>
        <p className="lead">
          The ultimate hope is that Reddit researchers, marketers, community mods or application builders will benefit from this dataset.
        </p>
        <p className="lead">
          The current placement is far from being perfect; we continue to make improvements, but the sheer volume of data requires
          crowdsourcing this effort.
        </p>
        {account?.login ? (
          <div>
            <Alert color="success">You are logged in as user &quot;{account.login}&quot;.</Alert>
          </div>
        ) : (
          <div>
            <Alert color="warning">
              Found data that looks off? Consider &nbsp;
              <Link to="/account/register" className="alert-link">
                registering
              </Link>{' '}
              a user account to get edit access.
            </Alert>
          </div>
        )}
        <p className="lead">
          Developers! If you find the API behind this site useful, consider giving a ⭐️ on{' '}
          <a href="https://github.com/singulart/subreddit-admin">Github</a>
        </p>
        <Alert color="info">Donations are appreciated and are help covering Amazon cloud expenses.</Alert>
      </Col>
    </Row>
  );
};

export default Home;

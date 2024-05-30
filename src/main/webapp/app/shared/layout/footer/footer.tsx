import './footer.scss';

import React from 'react';

import { Col, Row } from 'reactstrap';

const Footer = () => (
  <div className="footer page-content">
    <Row>
      <Col md="12">
        <p>
          <a href="https://buymeacoffee.com/0xlex">Buy me a coffee</a>
        </p>
      </Col>
    </Row>
  </div>
);

export default Footer;

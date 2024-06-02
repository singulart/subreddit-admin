import React, { useState, useEffect } from 'react';
//import './CookieConsent.css'; // Optional: for custom styling

const CookieConsent: React.FC = () => {
  const [visible, setVisible] = useState<boolean>(false);

  useEffect(() => {
    const consent = localStorage.getItem('cookieConsent');
    if (!consent) {
      setVisible(true);
    }
  }, []);

  const handleAccept = () => {
    localStorage.setItem('cookieConsent', 'true');
    function gtag(...args: any[]) {
      window.dataLayer.push(args);
    }
    gtag('consent', 'update', {
      ad_user_data: 'granted',
      ad_personalization: 'granted',
      ad_storage: 'granted',
      analytics_storage: 'granted',
    });

    setVisible(false);
  };

  return (
    visible && (
      <div className="cookie-consent">
        <p>We use cookies to ensure you get the best experience on our website. By continuing, you agree to our cookie policy.</p>
        <button onClick={handleAccept}>Accept</button>
      </div>
    )
  );
};

export default CookieConsent;

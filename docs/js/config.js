/**
 * CivicAge Survey Web Application - Configuration
 * Project: Smart Cities, Silent Citizens (ICSSR Major Research Project 2025-26)
 */

const CONFIG = {
  // Google Apps Script Web App Endpoint
  APPS_SCRIPT_URL: 'https://script.google.com/macros/s/AKfycbxJl6uhLpqFa-QkHuc9GFCrwTKaN_ju9JHbA-sy7O22pRrO9VzOJtsNqI4ZMdsrlyhb/exec',
  
  // Shared Security Secret Token
  SECRET_TOKEN: 'DCA_SECRET_TOKEN_2026',
  
  // Master Password - Entering this switches any session to Master role
  MASTER_PASSWORD: 'SCSCD@2026',
  
  // Master Session Duration (5 minutes)
  MASTER_SESSION_SECONDS: 300,
  
  // Role Constants
  ROLE_MASTER: 'master',
  ROLE_NORMAL: 'normal',
  ROLE_QA: 'qa',
  
  // Central Master Google Document (SCSC Data)
  COMMON_DOC_ID: '1pqmSuvsDRqnEQ-Yxw4Mh27-gYAEguwyLdrR6WvCCRU8',
  
  // 12 Dedicated Field Investigator Documents (SC0101 to SC0112)
  USER_DOC_MAP: {
    'SC0101': '1f6YxXWVt35bMnJsNzuHwzObN3N87WKsC38DAvIw4eCk',
    'SC0102': '1BMMXDiXvr95y17S_zvIsjI98Bg5FnY7Ks3OCj6mLlPo',
    'SC0103': '15wneFqNQz3OPL_tgMfnUY1JB_cFna6aNwzqxndRGaNs',
    'SC0104': '1YQccrd6KBvwUpa4fXRY9UpY_-XkKiNHkktGT-9FPHdU',
    'SC0105': '1hdtjT4itfIoHE8IUkYG7t4P6sZqt9g96Uinn_jcRNBk',
    'SC0106': '1VpSDVbP1Gw7KnWbb6FI4DGLycOprmv1xTz2OW9nS8lc',
    'SC0107': '1pJa9BoLLdc6Px0cmg02Zscm0k-ANaOjp0buY0LwopuM',
    'SC0108': '1ANCJrhiSAouc98XXhQR2waDEAV7rgHHxI1TX48Ddx1A',
    'SC0109': '1mL4maU2XkhKfTBeEXW2pElS_8ZTTzLFGsIHpHMfQ6LU',
    'SC0110': '1U_wBWwYESM4MZsi0c-Ifbbl66dyZ5sTCF34_ZteG6Hc',
    'SC0111': '1B6d4Chay-Q0Dxu6y9CD0gQsqo2LfcQhAR-Em0HBfQ4Q',
    'SC0112': '1qUnuEa_EjtEqXMQ88dlCOSSPgt9RfdF_vZPU50taV3g'
  },
  
  // 25 Field Surveyor Credentials: ID -> Password
  NORMAL_USER_CREDENTIALS: {
    'SC0101': 'scsc01',
    'SC0102': 'scsc02',
    'SC0103': 'scsc03',
    'SC0104': 'scsc04',
    'SC0105': 'scsc05',
    'SC0106': 'scsc06',
    'SC0107': 'scsc07',
    'SC0108': 'scsc08',
    'SC0109': 'scsc09',
    'SC0110': 'scsc10',
    'SC0111': 'scsc11',
    'SC0112': 'scsc12',
    'SC0113': 'scsc13',
    'SC0114': 'scsc14',
    'SC0115': 'scsc15',
    'SC0116': 'scsc16',
    'SC0117': 'scsc17',
    'SC0118': 'scsc18',
    'SC0119': 'scsc19',
    'SC0120': 'scsc20',
    'SC0121': 'scsc21',
    'SC0122': 'scsc22',
    'SC0123': 'scsc23',
    'SC0124': 'scsc24',
    'SC0125': 'scsc25'
  },
  
  // Research Cities
  CITIES: [
    { code: 'ND', name: 'New Delhi (National Capital Territory)' },
    { code: 'SG', name: 'Srinagar (Jammu & Kashmir)' },
    { code: 'AZ', name: 'Aizawl (Mizoram)' },
    { code: 'CH', name: 'Chandigarh (Union Territory)' },
    { code: 'GN', name: 'Gandhinagar (Gujarat)' },
    { code: 'IN', name: 'Indore (Madhya Pradesh)' }
  ],
  
  // City-Specific Smart City Services (for table grids)
  CITY_SERVICES: {
    'ND': ['Delhi Metro app', 'Delhi One app', 'EDMC e-services', 'Mohalla Clinic app', 'Smart parking'],
    'SG': ['Smart bus shelters', 'Wi-Fi hotspots', 'ICCC grievance portal', 'Smart waste management'],
    'AZ': ['Smart solid waste tracking', 'Sewerage monitoring portal', 'ICCC civic services'],
    'CH': ['Smart road services', 'E-bus tracking app', 'Digital health centres', 'Smart parks'],
    'GN': ['iNagar app', 'GIFT City infrastructure services', 'Smart health kiosks'],
    'IN': ['311 Smart City app', 'BRTS digital booking', 'ICCC grievance system', 'Digital waste compliance']
  },
  
  // Helper: Get Google Doc URL for a surveyor
  getDocUrlForSurveyor(surveyorId) {
    const cleanId = (surveyorId || '').trim().toUpperCase();
    const docId = this.USER_DOC_MAP[cleanId] || this.COMMON_DOC_ID;
    return `https://docs.google.com/document/d/${docId}/edit`;
  },
  
  // Helper: Get Master Central Doc URL
  getMasterDocUrl() {
    return `https://docs.google.com/document/d/${this.COMMON_DOC_ID}/edit`;
  },
  
  // Helper: Authenticate ID and password
  authenticate(id, password) {
    const cleanId = (id || '').trim().toUpperCase();
    const cleanPass = (password || '').trim();
    
    // Master password unlocks master role directly
    if (cleanPass === this.MASTER_PASSWORD) {
      return {
        role: this.ROLE_MASTER,
        id: cleanId.startsWith('SC01') ? cleanId : 'MASTER-ADMIN',
        name: cleanId.startsWith('SC01') ? `Master Investigator (${cleanId})` : 'Master Central Administrator'
      };
    }
    
    // Normal user validation
    const expectedPass = this.NORMAL_USER_CREDENTIALS[cleanId];
    if (expectedPass && expectedPass === cleanPass) {
      return {
        role: this.ROLE_NORMAL,
        id: cleanId,
        name: `Field Surveyor ${cleanId}`
      };
    }
    
    return null;
  }
};

// Freeze config to prevent accidental mutations
Object.freeze(CONFIG);
Object.freeze(CONFIG.USER_DOC_MAP);
Object.freeze(CONFIG.NORMAL_USER_CREDENTIALS);
Object.freeze(CONFIG.CITY_SERVICES);
if (typeof window !== 'undefined') window.CONFIG = CONFIG;


/**
 * CivicAge Survey Web Application - IndexedDB Database Service
 * Provides offline-first resilient storage for drafts, completed sessions, and cloud synced records.
 */

const DB = {
  dbName: 'CivicAgeDB',
  dbVersion: 1,
  _db: null,

  // Initialize IndexedDB
  async init() {
    if (this._db) return this._db;
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.dbVersion);

      request.onupgradeneeded = (event) => {
        const db = event.target.result;

        // Surveys Object Store
        if (!db.objectStoreNames.contains('surveys')) {
          const surveyStore = db.createObjectStore('surveys', { keyPath: 'surveyUid' });
          surveyStore.createIndex('status', 'status', { unique: false });
          surveyStore.createIndex('instrumentType', 'instrumentType', { unique: false });
          surveyStore.createIndex('surveyorId', 'surveyorId', { unique: false });
          surveyStore.createIndex('createdAt', 'createdAt', { unique: false });
          surveyStore.createIndex('isSynced', 'isSynced', { unique: false });
        }

        // Session Object Store
        if (!db.objectStoreNames.contains('session')) {
          db.createObjectStore('session', { keyPath: 'key' });
        }

        // Settings Object Store
        if (!db.objectStoreNames.contains('settings')) {
          db.createObjectStore('settings', { keyPath: 'key' });
        }
      };

      request.onsuccess = (event) => {
        this._db = event.target.result;
        resolve(this._db);
      };

      request.onerror = (event) => {
        console.error('IndexedDB open error:', event.target.error);
        reject(event.target.error);
      };
    });
  },

  // Save or update a single survey
  async saveSurvey(survey) {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['surveys'], 'readwrite');
      const store = tx.objectStore('surveys');

      // Guarantee timestamps and defaults
      const now = Date.now();
      const entity = {
        ...survey,
        updatedAt: now,
        createdAt: survey.createdAt || now
      };

      const req = store.put(entity);
      req.onsuccess = () => resolve(entity);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Retrieve survey by unique ID
  async getSurvey(surveyUid) {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['surveys'], 'readonly');
      const store = tx.objectStore('surveys');
      const req = store.get(surveyUid);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Retrieve all surveys sorted by createdAt desc
  async getAllSurveys() {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['surveys'], 'readonly');
      const store = tx.objectStore('surveys');
      const req = store.getAll();
      req.onsuccess = () => {
        const list = req.result || [];
        list.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
        resolve(list);
      };
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Delete survey by ID
  async deleteSurvey(surveyUid) {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['surveys'], 'readwrite');
      const store = tx.objectStore('surveys');
      const req = store.delete(surveyUid);
      req.onsuccess = () => resolve(true);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Safely upsert surveys preserving local answers and transcripts (matches SurveyRepository.upsertPreservingLocal)
  async upsertSurveys(remoteList) {
    if (!Array.isArray(remoteList) || remoteList.length === 0) return 0;
    const db = await this.init();
    let updatedCount = 0;

    for (const remote of remoteList) {
      if (!remote.recordId && !remote.surveyUid) continue;
      const uid = remote.recordId || remote.surveyUid;
      const existing = await this.getSurvey(uid);

      let toSave;
      if (existing) {
        toSave = {
          ...existing,
          status: remote.status || existing.status,
          qaNotes: remote.qaNotes || existing.qaNotes,
          approvedBy: remote.approvedBy || existing.approvedBy,
          approvedAt: remote.approvedAt || existing.approvedAt,
          answersJson: (remote.answersJson && remote.answersJson !== '{}') ? remote.answersJson : existing.answersJson,
          transcriptText: remote.transcriptText || existing.transcriptText,
          manualSummary: remote.manualSummary || existing.manualSummary,
          surveyorId: remote.surveyorId || remote.userId || existing.surveyorId,
          surveyorName: remote.displayName || remote.surveyorName || existing.surveyorName,
          participantName: remote.participantName || existing.participantName,
          city: remote.city || existing.city,
          manualLocality: remote.manualLocality || existing.manualLocality,
          isSynced: true,
          updatedAt: Math.max(existing.updatedAt || 0, Date.now())
        };
      } else {
        toSave = {
          surveyUid: uid,
          recordId: uid,
          surveyorId: remote.surveyorId || remote.userId || '',
          surveyorName: remote.displayName || remote.surveyorName || `Field Surveyor ${remote.userId || ''}`,
          createdBy: remote.createdBy || remote.userId || '',
          participantName: remote.participantName || 'Anonymous',
          participantAge: remote.participantAge || 0,
          city: remote.city || 'ND',
          manualLocality: remote.manualLocality || '',
          instrumentType: remote.instrumentType || '',
          latitude: remote.geoLat || 0,
          longitude: remote.geoLng || 0,
          gpsAccuracy: remote.geoAccuracy || 0,
          consentGiven: remote.consentGiven !== false,
          status: remote.status || (remote.approved ? 'approved' : 'submitted'),
          qaNotes: remote.qaNotes || '',
          approvedBy: remote.approvedBy || '',
          approvedAt: remote.approvedAt || 0,
          answersJson: typeof remote.answers === 'object' ? JSON.stringify(remote.answers) : (remote.answersJson || '{}'),
          transcriptText: remote.transcriptText || '',
          manualSummary: remote.manualSummary || '',
          createdAt: remote.createdAt ? new Date(remote.createdAt).getTime() : Date.now(),
          updatedAt: Date.now(),
          isSynced: true
        };
      }

      await this.saveSurvey(toSave);
      updatedCount++;
    }

    return updatedCount;
  },

  // Save current user session
  async saveSession(sessionData) {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['session'], 'readwrite');
      const store = tx.objectStore('session');
      const req = store.put({ key: 'currentUser', ...sessionData });
      req.onsuccess = () => resolve(true);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Get current user session
  async getSession() {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['session'], 'readonly');
      const store = tx.objectStore('session');
      const req = store.get('currentUser');
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Clear current user session
  async clearSession() {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['session'], 'readwrite');
      const store = tx.objectStore('session');
      const req = store.delete('currentUser');
      req.onsuccess = () => resolve(true);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Save setting key/value
  async setSetting(key, value) {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['settings'], 'readwrite');
      const store = tx.objectStore('settings');
      const req = store.put({ key, value });
      req.onsuccess = () => resolve(true);
      req.onerror = (e) => reject(e.target.error);
    });
  },

  // Get setting by key
  async getSetting(key) {
    const db = await this.init();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(['settings'], 'readonly');
      const store = tx.objectStore('settings');
      const req = store.get(key);
      req.onsuccess = () => resolve(req.result ? req.result.value : null);
      req.onerror = (e) => reject(e.target.error);
    });
  }
};
if (typeof window !== 'undefined') window.DB = DB;


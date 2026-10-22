/**
 * CivicAge Survey Web Application - Main Application Controller
 * High-Precision Standalone PWA Architecture
 */

class CivicAgeApp {
  constructor() {
    this.currentView = 'login';
    this.currentUser = null;
    this.masterTimer = null;
    this.masterSecondsRemaining = 0;
    
    // Active survey session state
    this.activeSurvey = null;
    this.currentInstrument = null;
    this.currentSectionIndex = 0;
    this.currentQuestionIndex = 0;
    this.currentThemeIndex = 0;
    this.showingThemes = false;
    this.surveyAnswers = {};
    this.probeResponses = {};
    this.selectedProbes = {};

    // Speech recognition
    this.speechRecognizer = null;
    this.isRecordingVoice = false;

    // Signature pad state
    this.signatureCanvas = null;
    this.isDrawingSignature = false;
    this.hasSignature = false;

    // Location state
    this.currentGps = { lat: 0, lng: 0, accuracy: 0, timestamp: 0, status: 'Not acquired' };
  }

  async init() {
    // Check saved session
    const savedUser = await DB.getSession();
    if (savedUser && savedUser.id) {
      this.currentUser = savedUser;
      if (this.currentUser.role === CONFIG.ROLE_MASTER) {
        this.startMasterCountdown(savedUser.remainingSeconds || CONFIG.MASTER_SESSION_SECONDS);
      }
      this.navigate('dashboard');
    } else {
      this.navigate('login');
    }

    // Load theme preference
    const savedTheme = await DB.getSetting('theme');
    if (savedTheme) {
      document.documentElement.setAttribute('data-theme', savedTheme);
    }

    // Set up network listeners
    window.addEventListener('online', () => this.handleNetworkChange(true));
    window.addEventListener('offline', () => this.handleNetworkChange(false));

    // Initialize speech recognition if available
    this.initSpeechRecognition();
  }

  // ── Router & Navigation ──────────────────────────────────────────────────
  navigate(viewName, params = {}) {
    this.currentView = viewName;
    const mainEl = document.getElementById('view-container');
    if (!mainEl) return;

    // Render corresponding view
    switch (viewName) {
      case 'login':
        mainEl.innerHTML = this.renderLoginView();
        this.bindLoginEvents();
        break;
      case 'dashboard':
        mainEl.innerHTML = this.renderDashboardView();
        this.bindDashboardEvents();
        break;
      case 'instruments':
        mainEl.innerHTML = this.renderInstrumentsView();
        this.bindInstrumentsEvents();
        break;
      case 'intake':
        mainEl.innerHTML = this.renderIntakeView(params.instrumentCode);
        this.bindIntakeEvents(params.instrumentCode);
        break;
      case 'consent':
        mainEl.innerHTML = this.renderConsentView();
        this.bindConsentEvents();
        break;
      case 'runner':
        mainEl.innerHTML = this.renderRunnerView();
        this.bindRunnerEvents();
        break;
      case 'summary':
        mainEl.innerHTML = this.renderSummaryView();
        this.bindSummaryEvents();
        break;
      case 'records':
        mainEl.innerHTML = this.renderRecordsView();
        this.bindRecordsEvents();
        break;
      default:
        mainEl.innerHTML = this.renderDashboardView();
        this.bindDashboardEvents();
    }

    this.updateNavbar();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  updateNavbar() {
    const userRoleEl = document.getElementById('nav-user-info');
    const masterPillEl = document.getElementById('nav-master-pill');
    const navActionsEl = document.getElementById('nav-actions-container');

    if (!this.currentUser) {
      if (userRoleEl) userRoleEl.style.display = 'none';
      if (masterPillEl) masterPillEl.style.display = 'none';
      if (navActionsEl) navActionsEl.style.display = 'none';
      return;
    }

    if (navActionsEl) navActionsEl.style.display = 'flex';
    if (userRoleEl) {
      userRoleEl.style.display = 'block';
      userRoleEl.innerHTML = `
        <span style="font-weight:700;">${this.currentUser.id}</span>
        <span class="badge ${this.currentUser.role === CONFIG.ROLE_MASTER ? 'badge-rose' : 'badge-primary'}">${this.currentUser.role.toUpperCase()}</span>
      `;
    }

    if (masterPillEl) {
      if (this.currentUser.role === CONFIG.ROLE_MASTER && this.masterSecondsRemaining > 0) {
        masterPillEl.style.display = 'inline-flex';
        const min = Math.floor(this.masterSecondsRemaining / 60);
        const sec = this.masterSecondsRemaining % 60;
        masterPillEl.innerHTML = `
          <span>MASTER</span>
          <span>${min}:${sec < 10 ? '0' : ''}${sec}</span>
        `;
        if (this.masterSecondsRemaining <= 60) {
          masterPillEl.classList.add('expiring');
        } else {
          masterPillEl.classList.remove('expiring');
        }
      } else {
        masterPillEl.style.display = 'none';
      }
    }
  }

  // ── Authentication ───────────────────────────────────────────────────────
  async login(id, password) {
    const authResult = CONFIG.authenticate(id, password);
    if (!authResult) {
      this.toast('Invalid ID or Password. (Field: SC0101–SC0125 / Master: SCSCD@2026)', 'error');
      return false;
    }

    this.currentUser = {
      id: authResult.id,
      name: authResult.name,
      role: authResult.role,
      loginTimestamp: Date.now()
    };

    if (authResult.role === CONFIG.ROLE_MASTER) {
      this.startMasterCountdown(CONFIG.MASTER_SESSION_SECONDS);
    }

    await DB.saveSession(this.currentUser);
    this.toast(`Welcome, ${authResult.name}!`, 'success');
    this.navigate('dashboard');
    return true;
  }

  async logout() {
    this.stopMasterCountdown();
    this.currentUser = null;
    await DB.clearSession();
    this.toast('Logged out successfully', 'info');
    this.navigate('login');
  }

  startMasterCountdown(seconds) {
    this.stopMasterCountdown();
    this.masterSecondsRemaining = seconds;
    this.masterTimer = setInterval(() => {
      this.masterSecondsRemaining--;
      this.updateNavbar();

      if (this.masterSecondsRemaining <= 0) {
        this.stopMasterCountdown();
        this.demoteMasterRole();
      }
    }, 1000);
  }

  stopMasterCountdown() {
    if (this.masterTimer) {
      clearInterval(this.masterTimer);
      this.masterTimer = null;
    }
    this.masterSecondsRemaining = 0;
    this.updateNavbar();
  }

  async demoteMasterRole() {
    if (!this.currentUser) return;
    this.toast('Master session expired. Switching back to Field Surveyor mode.', 'warning');
    this.currentUser.role = CONFIG.ROLE_NORMAL;
    await DB.saveSession(this.currentUser);
    this.updateNavbar();
    if (this.currentView === 'records') {
      this.navigate('records');
    }
  }

  async switchToMasterWithPassword(pass) {
    if ((pass || '').trim() !== CONFIG.MASTER_PASSWORD) {
      this.toast('Incorrect Master Password', 'error');
      return false;
    }

    this.currentUser.role = CONFIG.ROLE_MASTER;
    this.startMasterCountdown(CONFIG.MASTER_SESSION_SECONDS);
    await DB.saveSession(this.currentUser);
    this.toast('Master Mode Activated (5 min session)', 'success');
    this.updateNavbar();
    return true;
  }

  // ── Views Rendering ──────────────────────────────────────────────────────

  // 1. Login View
  renderLoginView() {
    return `
      <div class="fade-in" style="max-width: 480px; margin: 3rem auto;">
        <div class="card" style="padding: 2.25rem;">
          <div style="text-align: center; margin-bottom: 2rem;">
            <div class="nav-logo-icon" style="width: 58px; height: 58px; margin: 0 auto 1rem; font-size: 1.5rem;">
              🏛️
            </div>
            <h1 style="font-family: var(--font-display); font-size: 1.65rem; font-weight: 700;">CivicAge Survey</h1>
            <p style="color: var(--text-muted); font-size: 0.85rem; margin-top: 0.25rem;">
              ICSSR Major Research Project 2025–26<br>
              <strong>Smart Cities, Silent Citizens</strong>
            </p>
          </div>

          <form id="login-form">
            <div class="form-group">
              <label class="form-label" for="login-id">Surveyor ID / User ID</label>
              <input type="text" id="login-id" class="form-control" placeholder="e.g. SC0101 or SC0102" required autofocus>
              <span style="font-size: 0.72rem; color: var(--text-subtle);">Authorized IDs: SC0101 to SC0125</span>
            </div>

            <div class="form-group">
              <label class="form-label" for="login-password">Password / Master Key</label>
              <input type="password" id="login-password" class="form-control" placeholder="Enter password" required>
            </div>

            <button type="submit" class="btn btn-primary btn-lg" style="width: 100%; margin-top: 1rem;">
              Sign In to Field Portal
            </button>
          </form>

          <div style="margin-top: 1.75rem; padding-top: 1.25rem; border-top: 1px solid var(--border-glass); text-align: center;">
            <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">Quick Demo Fillers:</p>
            <div style="display: flex; gap: 0.5rem; justify-content: center; flex-wrap: wrap;">
              <button class="btn btn-outline btn-sm" onclick="app.quickFillLogin('SC0101', 'scsc01')">Surveyor SC0101</button>
              <button class="btn btn-outline btn-sm" onclick="app.quickFillLogin('SC0102', 'scsc02')">Surveyor SC0102</button>
              <button class="btn btn-rose btn-sm" onclick="app.quickFillLogin('SC0101', '${CONFIG.MASTER_PASSWORD}')">Master Login</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  bindLoginEvents() {
    const form = document.getElementById('login-form');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('login-id').value;
      const pass = document.getElementById('login-password').value;
      await this.login(id, pass);
    });
  }

  quickFillLogin(id, pass) {
    document.getElementById('login-id').value = id;
    document.getElementById('login-password').value = pass;
    this.login(id, pass);
  }

  // 2. Dashboard View
  renderDashboardView() {
    const isMaster = this.currentUser && this.currentUser.role === CONFIG.ROLE_MASTER;
    return `
      <div class="fade-in">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
          <div>
            <h1 style="font-family: var(--font-display); font-size: 1.85rem; font-weight: 800;">
              ${isMaster ? 'DCA Master Control Dashboard' : 'Field Surveyor Dashboard'}
            </h1>
            <p style="color: var(--text-muted); font-size: 0.9rem;">
              ${isMaster ? 'Central audit reviewer & Google Doc synchronization engine' : 'Conduct high-accuracy geotagged interviews & surveys'}
            </p>
          </div>
          <div style="display: flex; gap: 0.75rem;">
            ${!isMaster ? `
              <button class="btn btn-outline btn-sm" onclick="app.showMasterSwitchModal()">
                🔒 Switch to Master
              </button>
            ` : `
              <button class="btn btn-rose btn-sm" onclick="app.demoteMasterRole()">
                End Master Session
              </button>
            `}
          </div>
        </div>

        <!-- Real-Time Metrics Counters -->
        <div id="dashboard-stats-container" class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(99, 102, 241, 0.15); color: #818cf8;">📊</div>
            <div>
              <div class="stat-val" id="stat-total">0</div>
              <div class="stat-lbl">Total Records</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(245, 158, 11, 0.15); color: #fbbf24;">⏳</div>
            <div>
              <div class="stat-val" id="stat-pending">0</div>
              <div class="stat-lbl">Pending Review</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(16, 185, 129, 0.15); color: #34d399;">✅</div>
            <div>
              <div class="stat-val" id="stat-approved">0</div>
              <div class="stat-lbl">Approved in Docs</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(14, 165, 233, 0.15); color: #38bdf8;">📝</div>
            <div>
              <div class="stat-val" id="stat-drafts">0</div>
              <div class="stat-lbl">Local Drafts</div>
            </div>
          </div>
        </div>

        <!-- Action Cards Grid -->
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; margin-top: 1rem;">
          <div class="card card-interactive" onclick="app.navigate('instruments')">
            <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 0.75rem;">
              <div class="stat-icon" style="background: linear-gradient(135deg, var(--primary), var(--accent)); color: white; width: 44px; height: 44px;">➕</div>
              <h3 style="font-size: 1.15rem; font-weight: 700;">Start Research Instrument</h3>
            </div>
            <p style="color: var(--text-muted); font-size: 0.88rem; margin-bottom: 1rem;">
              Choose from all 15 quantitative, qualitative, mixed, and observational instruments. Auto-captures GPS and signed consent.
            </p>
            <span class="badge badge-primary">15 Instruments Available →</span>
          </div>

          <div class="card card-interactive" onclick="app.navigate('records')">
            <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 0.75rem;">
              <div class="stat-icon" style="background: rgba(16, 185, 129, 0.2); color: #34d399; width: 44px; height: 44px;">📂</div>
              <h3 style="font-size: 1.15rem; font-weight: 700;">Records & Review Board</h3>
            </div>
            <p style="color: var(--text-muted); font-size: 0.88rem; margin-bottom: 1rem;">
              ${isMaster ? 'Inspect, approve, and route responses directly to dedicated Field Docs & Central Master Audit.' : 'View your submitted sessions, drafts, and approval status.'}
            </p>
            <span class="badge ${isMaster ? 'badge-rose' : 'badge-emerald'}">${isMaster ? 'Master Approval Mode' : 'View My Records'} →</span>
          </div>
        </div>

        <!-- Double-Doc Engine Status Banner -->
        <div class="card" style="margin-top: 1.5rem; background: rgba(99, 102, 241, 0.05); border-color: rgba(99, 102, 241, 0.2);">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
            <div>
              <h4 style="font-weight: 700; color: #818cf8; margin-bottom: 0.25rem;">Double-Doc Routing Engine Active</h4>
              <p style="font-size: 0.82rem; color: var(--text-muted);">
                - Field Docs: Full Q&A & transcripts written to <strong>${this.currentUser.id}</strong> (or designated SC0101–SC0112)<br>
                - Master Central Doc: Official Approval Audit Log appended to <strong>COMMON_DOC_ID</strong>
              </p>
            </div>
            <div style="display: flex; gap: 0.5rem;">
              <a href="${CONFIG.getDocUrlForSurveyor(this.currentUser.id)}" target="_blank" rel="noopener noreferrer" class="btn btn-outline btn-sm">
                📄 Open My Field Doc
              </a>
              ${isMaster ? `
                <a href="${CONFIG.getMasterDocUrl()}" target="_blank" rel="noopener noreferrer" class="btn btn-rose btn-sm">
                  📑 Open Central Master Doc
                </a>
              ` : ''}
            </div>
          </div>
        </div>
      </div>
    `;
  }

  async bindDashboardEvents() {
    // Load and update stats
    const allSurveys = await DB.getAllSurveys();
    const total = allSurveys.length;
    const drafts = allSurveys.filter(s => s.status === 'DRAFT').length;
    const approved = allSurveys.filter(s => s.status === 'approved').length;
    const pending = allSurveys.filter(s => s.status === 'submitted' || s.status === 'COMPLETED').length;

    const elTotal = document.getElementById('stat-total');
    const elDrafts = document.getElementById('stat-drafts');
    const elApproved = document.getElementById('stat-approved');
    const elPending = document.getElementById('stat-pending');

    if (elTotal) elTotal.innerText = total;
    if (elDrafts) elDrafts.innerText = drafts;
    if (elApproved) elApproved.innerText = approved;
    if (elPending) elPending.innerText = pending;

    // Auto-fetch cloud records if Master
    if (this.currentUser && this.currentUser.role === CONFIG.ROLE_MASTER) {
      API.fetchCloudRecords().then(res => {
        if (res && res.syncedCount > 0) {
          this.toast(`Synced ${res.syncedCount} cloud records into Master board`, 'info');
        }
      });
    }
  }

  // 3. Instruments Selection View
  renderInstrumentsView() {
    const categories = ['ALL', 'QUANTITATIVE', 'QUALITATIVE', 'MIXED', 'OBSERVATIONAL', 'DOCTRINAL'];

    return `
      <div class="fade-in">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
          <div>
            <h1 style="font-family: var(--font-display); font-size: 1.75rem; font-weight: 800;">Research Instruments</h1>
            <p style="color: var(--text-muted); font-size: 0.88rem;">Select an instrument to begin a geotagged survey session</p>
          </div>
          <button class="btn btn-outline btn-sm" onclick="app.navigate('dashboard')">← Back to Dashboard</button>
        </div>

        <!-- Filter Pills -->
        <div style="display: flex; gap: 0.5rem; overflow-x: auto; padding-bottom: 0.5rem; margin-bottom: 1.5rem;">
          ${categories.map(cat => `
            <button class="btn btn-sm btn-outline filter-btn" data-category="${cat}" onclick="app.filterInstruments('${cat}')">
              ${cat}
            </button>
          `).join('')}
        </div>

        <!-- Instruments Grid -->
        <div id="instruments-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 1.25rem;">
          ${INSTRUMENT_BANK.instruments.map(inst => `
            <div class="card card-interactive instrument-card" data-type="${inst.type}" onclick="app.startIntake('${inst.code}')">
              <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                <span class="badge ${this.getBadgeClassForType(inst.type)}">${inst.type}</span>
                <span style="font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-subtle);">${inst.code}</span>
              </div>
              <h3 style="font-size: 1.1rem; font-weight: 700; margin-bottom: 0.35rem;">${inst.title}</h3>
              <p style="font-size: 0.78rem; color: #818cf8; font-weight: 600; margin-bottom: 0.65rem;">Stakeholder: ${inst.stakeholder}</p>
              <p style="font-size: 0.82rem; color: var(--text-muted); line-height: 1.4; margin-bottom: 1rem;">
                ${inst.description || 'Structured research protocol.'}
              </p>
              <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-glass); padding-top: 0.75rem; font-size: 0.78rem; color: var(--text-subtle);">
                <span>${inst.sections ? `${inst.sections.length} Sections` : `${inst.themes ? inst.themes.length : 0} Themes`}</span>
                <span style="font-weight: 600; color: var(--primary);">Begin Session →</span>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  getBadgeClassForType(type) {
    switch (type) {
      case 'QUANTITATIVE': return 'badge-primary';
      case 'QUALITATIVE': return 'badge-sky';
      case 'MIXED': return 'badge-emerald';
      case 'OBSERVATIONAL': return 'badge-amber';
      case 'DOCTRINAL': return 'badge-rose';
      default: return 'badge-primary';
    }
  }

  filterInstruments(category) {
    const cards = document.querySelectorAll('.instrument-card');
    cards.forEach(card => {
      if (category === 'ALL' || card.getAttribute('data-type') === category) {
        card.style.display = 'block';
      } else {
        card.style.display = 'none';
      }
    });

    document.querySelectorAll('.filter-btn').forEach(btn => {
      if (btn.getAttribute('data-category') === category) {
        btn.classList.add('btn-primary');
        btn.classList.remove('btn-outline');
      } else {
        btn.classList.remove('btn-primary');
        btn.classList.add('btn-outline');
      }
    });
  }

  bindInstrumentsEvents() {
    this.filterInstruments('ALL');
  }

  // 4. Survey Intake View (Participant & GPS)
  startIntake(instrumentCode) {
    this.currentInstrument = INSTRUMENT_BANK.getInstrument(instrumentCode);
    if (!this.currentInstrument) {
      this.toast('Instrument not found', 'error');
      return;
    }
    this.navigate('intake', { instrumentCode });
  }

  renderIntakeView(instrumentCode) {
    const inst = this.currentInstrument || INSTRUMENT_BANK.getInstrument(instrumentCode);
    return `
      <div class="fade-in" style="max-width: 680px; margin: 0 auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
          <div>
            <span class="badge badge-primary">${inst.type}</span>
            <h1 style="font-family: var(--font-display); font-size: 1.65rem; font-weight: 700; margin-top: 0.25rem;">
              ${inst.title}
            </h1>
            <p style="color: var(--text-muted); font-size: 0.85rem;">Step 1: Participant Intake & GPS Verification</p>
          </div>
          <button class="btn btn-outline btn-sm" onclick="app.navigate('instruments')">Cancel</button>
        </div>

        <form id="intake-form">
          <div class="card">
            <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 1.25rem; color: #818cf8;">Participant Details</h3>

            <div class="form-group">
              <label class="form-label" for="intake-name">Participant Full Name / Code *</label>
              <input type="text" id="intake-name" class="form-control" placeholder="e.g. Ramesh Chandra or OOP-ND-101" required>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
              <div class="form-group">
                <label class="form-label" for="intake-age">Age (Years) *</label>
                <input type="number" id="intake-age" class="form-control" placeholder="e.g. 78" min="18" max="120" required>
              </div>
              <div class="form-group">
                <label class="form-label" for="intake-gender">Gender *</label>
                <select id="intake-gender" class="form-control form-select" required>
                  <option value="">Select Gender</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Transgender">Transgender</option>
                  <option value="Prefer not to say">Prefer not to say</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="intake-phone">Contact Phone (Optional)</label>
              <input type="tel" id="intake-phone" class="form-control" placeholder="10-digit mobile number">
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
              <div class="form-group">
                <label class="form-label" for="intake-city">City Location *</label>
                <select id="intake-city" class="form-control form-select" required>
                  ${CONFIG.CITIES.map(c => `
                    <option value="${c.code}" ${c.code === 'ND' ? 'selected' : ''}>${c.name}</option>
                  `).join('')}
                </select>
              </div>
              <div class="form-group">
                <label class="form-label" for="intake-locality">Specific Locality / Ward *</label>
                <input type="text" id="intake-locality" class="form-control" placeholder="e.g. Karol Bagh Ward 12" required>
              </div>
            </div>
          </div>

          <!-- High-Accuracy Geotagging Card -->
          <div class="card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
              <div>
                <h3 style="font-size: 1.05rem; font-weight: 700; color: #34d399;">High-Accuracy GPS Geotag</h3>
                <p style="font-size: 0.78rem; color: var(--text-muted);">Required for ICSSR field validation</p>
              </div>
              <button type="button" class="btn btn-outline btn-sm" id="btn-acquire-gps" onclick="app.acquireGps()">
                📡 Acquire GPS
              </button>
            </div>

            <div style="background: rgba(15, 23, 42, 0.4); border: 1px solid var(--border-glass); border-radius: var(--radius-md); padding: 1rem;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-size: 0.85rem; color: var(--text-muted);">GPS Coordinates:</span>
                <span id="gps-coords-display" style="font-family: var(--font-mono); font-size: 0.9rem; font-weight: 600;">
                  Lat: 0.000000, Lng: 0.000000
                </span>
              </div>
              <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.5rem;">
                <span style="font-size: 0.85rem; color: var(--text-muted);">Accuracy:</span>
                <span id="gps-accuracy-badge" class="badge badge-amber">GPS Not Acquired</span>
              </div>
            </div>
          </div>

          <div style="display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem;">
            <button type="button" class="btn btn-outline" onclick="app.navigate('instruments')">Back</button>
            <button type="submit" class="btn btn-primary btn-lg">Proceed to Informed Consent →</button>
          </div>
        </form>
      </div>
    `;
  }

  bindIntakeEvents(instrumentCode) {
    // Acquire GPS immediately on load
    this.acquireGps();

    const form = document.getElementById('intake-form');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      const name = document.getElementById('intake-name').value.trim();
      const age = parseInt(document.getElementById('intake-age').value, 10);
      const gender = document.getElementById('intake-gender').value;
      const phone = document.getElementById('intake-phone').value.trim();
      const city = document.getElementById('intake-city').value;
      const locality = document.getElementById('intake-locality').value.trim();

      // Check SQ-OOP and SSI-OOP 2-day separation protocol
      if (this.currentInstrument.code === 'SSI-OOP-v1.0') {
        const surveys = await DB.getAllSurveys();
        const recentSq = surveys.find(s => 
          s.instrumentType === 'SQ-OOP-v1.0' && 
          s.participantName.toLowerCase() === name.toLowerCase() && 
          (Date.now() - s.createdAt) < (2 * 24 * 60 * 60 * 1000)
        );
        if (recentSq) {
          const proceed = confirm("Protocol Alert: A 2-day gap is recommended between SQ-OOP and SSI-OOP for the same participant. Do you wish to override and continue?");
          if (!proceed) return;
        }
      }

      // Generate UID
      const sId = this.currentUser.id || 'SC0101';
      const timestamp = Date.now();
      const uid = `${city}-${sId}-${this.currentInstrument.code}-S${timestamp}`;

      // Initialize active survey entity
      this.activeSurvey = {
        surveyUid: uid,
        recordId: uid,
        surveyorId: sId,
        surveyorName: this.currentUser.name || `Field Investigator ${sId}`,
        createdBy: sId,
        participantName: name,
        participantAge: age,
        participantGender: gender,
        participantPhone: phone,
        city: city,
        manualLocality: locality,
        instrumentType: this.currentInstrument.code,
        latitude: this.currentGps.lat,
        longitude: this.currentGps.lng,
        gpsAccuracy: this.currentGps.accuracy,
        geoTimestamp: this.currentGps.timestamp || timestamp,
        consentGiven: true,
        photoConsent: false,
        audioConsent: false,
        geoLocationConsent: true,
        dataUseConsent: true,
        participantCanWithdraw: true,
        status: 'DRAFT',
        answersJson: '{}',
        transcriptText: '',
        manualSummary: '',
        qaNotes: '',
        createdAt: timestamp,
        updatedAt: timestamp,
        isSynced: false
      };

      this.surveyAnswers = {};
      this.probeResponses = {};
      this.selectedProbes = {};

      // Auto-save draft
      await DB.saveSurvey(this.activeSurvey);
      this.navigate('consent');
    });
  }

  acquireGps() {
    const coordsEl = document.getElementById('gps-coords-display');
    const badgeEl = document.getElementById('gps-accuracy-badge');

    if (!navigator.geolocation) {
      if (badgeEl) {
        badgeEl.className = 'badge badge-rose';
        badgeEl.innerText = 'Geolocation Not Supported';
      }
      return;
    }

    if (badgeEl) {
      badgeEl.className = 'badge badge-amber';
      badgeEl.innerText = 'Acquiring Satellite Lock...';
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        const acc = Math.round(position.coords.accuracy);

        this.currentGps = {
          lat: parseFloat(lat.toFixed(6)),
          lng: parseFloat(lng.toFixed(6)),
          accuracy: acc,
          timestamp: position.timestamp,
          status: 'Acquired'
        };

        if (coordsEl) {
          coordsEl.innerText = `Lat: ${this.currentGps.lat}, Lng: ${this.currentGps.lng}`;
        }
        if (badgeEl) {
          if (acc <= 20) {
            badgeEl.className = 'badge badge-emerald';
            badgeEl.innerText = `GPS High Accuracy (±${acc}m)`;
          } else if (acc <= 60) {
            badgeEl.className = 'badge badge-primary';
            badgeEl.innerText = `GPS Moderate (±${acc}m)`;
          } else {
            badgeEl.className = 'badge badge-amber';
            badgeEl.innerText = `GPS Approximate (±${acc}m)`;
          }
        }
      },
      (error) => {
        console.warn('Geolocation acquisition error:', error);
        if (badgeEl) {
          badgeEl.className = 'badge badge-rose';
          badgeEl.innerText = `GPS Error: ${error.message}`;
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 10000
      }
    );
  }

  // 5. Consent Flow View with Signature Canvas
  renderConsentView() {
    return `
      <div class="fade-in" style="max-width: 680px; margin: 0 auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
          <div>
            <span class="badge badge-emerald">Step 2: Ethical Clearance</span>
            <h1 style="font-family: var(--font-display); font-size: 1.65rem; font-weight: 700; margin-top: 0.25rem;">
              Informed Consent & Voluntary Participation
            </h1>
            <p style="color: var(--text-muted); font-size: 0.85rem;">Protocol Code: ${this.currentInstrument.consentCode || 'CONSENT-GEN'}</p>
          </div>
        </div>

        <div class="card" style="margin-bottom: 1.25rem;">
          <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.75rem; color: #818cf8;">Study Information</h3>
          <p style="font-size: 0.88rem; color: var(--text-main); line-height: 1.6; margin-bottom: 1rem;">
            This research study investigates how Smart City digital public infrastructure affects older persons (75+),
            their independence, and civic access. Participation is voluntary, confidential, and you may discontinue
            at any time without negative consequences.
          </p>

          <div style="display: flex; flex-direction: column; gap: 0.75rem;">
            <label class="choice-tile" style="margin: 0;">
              <input type="checkbox" id="consent-main" checked style="accent-color: var(--primary); width: 18px; height: 18px;">
              <span style="font-size: 0.88rem;">I voluntarily consent to participate in this research interview / survey.</span>
            </label>

            <label class="choice-tile" style="margin: 0;">
              <input type="checkbox" id="consent-audio" checked style="accent-color: var(--primary); width: 18px; height: 18px;">
              <span style="font-size: 0.88rem;">I agree to audio recording / voice dictation of responses for academic transcription.</span>
            </label>

            <label class="choice-tile" style="margin: 0;">
              <input type="checkbox" id="consent-geo" checked style="accent-color: var(--primary); width: 18px; height: 18px;">
              <span style="font-size: 0.88rem;">I agree to GPS geotag verification for research location mapping.</span>
            </label>

            <label class="choice-tile" style="margin: 0;">
              <input type="checkbox" id="consent-withdraw" checked style="accent-color: var(--primary); width: 18px; height: 18px;">
              <span style="font-size: 0.88rem;">The right to skip questions or withdraw at any moment was explained to me.</span>
            </label>
          </div>
        </div>

        <!-- Digital Signature Canvas -->
        <div class="card">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
            <h3 style="font-size: 1.05rem; font-weight: 700;">Participant Signature / Verbal Assent</h3>
            <button type="button" class="btn btn-outline btn-sm" onclick="app.clearSignature()">Clear Pad</button>
          </div>
          <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">
            Sign on screen or touch pad. If participant cannot sign physically, FI may record verbal assent below.
          </p>

          <div class="signature-box">
            <canvas id="signatureCanvas"></canvas>
            <div class="signature-actions">
              <span style="font-size: 0.75rem; color: var(--text-subtle);">Sign above with finger or stylus</span>
              <button type="button" class="btn btn-sm btn-outline" onclick="app.recordVerbalAssent()">Record Verbal Assent</button>
            </div>
          </div>
        </div>

        <div style="display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem;">
          <button type="button" class="btn btn-outline" onclick="app.navigate('intake', { instrumentCode: app.currentInstrument.code })">Back</button>
          <button type="button" class="btn btn-emerald btn-lg" onclick="app.confirmConsentAndStartRunner()">
            Confirm Consent & Begin Questions →
          </button>
        </div>
      </div>
    `;
  }

  bindConsentEvents() {
    this.initSignatureCanvas();
  }

  initSignatureCanvas() {
    const canvas = document.getElementById('signatureCanvas');
    if (!canvas) return;

    this.signatureCanvas = canvas;
    const ctx = canvas.getContext('2d');
    
    // Resize canvas properly for high-dpi
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * 2;
    canvas.height = rect.height * 2;
    ctx.scale(2, 2);
    ctx.lineWidth = 2.5;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#6366f1';

    const getPos = (e) => {
      const cRect = canvas.getBoundingClientRect();
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const clientY = e.touches ? e.touches[0].clientY : e.clientY;
      return {
        x: clientX - cRect.left,
        y: clientY - cRect.top
      };
    };

    const startDraw = (e) => {
      this.isDrawingSignature = true;
      this.hasSignature = true;
      const pos = getPos(e);
      ctx.beginPath();
      ctx.moveTo(pos.x, pos.y);
    };

    const draw = (e) => {
      if (!this.isDrawingSignature) return;
      e.preventDefault();
      const pos = getPos(e);
      ctx.lineTo(pos.x, pos.y);
      ctx.stroke();
    };

    const stopDraw = () => {
      this.isDrawingSignature = false;
    };

    canvas.addEventListener('mousedown', startDraw);
    canvas.addEventListener('mousemove', draw);
    window.addEventListener('mouseup', stopDraw);

    canvas.addEventListener('touchstart', startDraw, { passive: false });
    canvas.addEventListener('touchmove', draw, { passive: false });
    window.addEventListener('touchend', stopDraw);
  }

  clearSignature() {
    if (!this.signatureCanvas) return;
    const ctx = this.signatureCanvas.getContext('2d');
    ctx.clearRect(0, 0, this.signatureCanvas.width, this.signatureCanvas.height);
    this.hasSignature = false;
  }

  recordVerbalAssent() {
    if (!this.signatureCanvas) return;
    const ctx = this.signatureCanvas.getContext('2d');
    this.clearSignature();
    ctx.font = '16px Plus Jakarta Sans';
    ctx.fillStyle = '#10b981';
    ctx.fillText('✓ Oral / Verbal Assent Witnessed by Field Investigator', 20, 90);
    this.hasSignature = true;
    this.toast('Oral assent recorded', 'info');
  }

  async confirmConsentAndStartRunner() {
    const mainConsent = document.getElementById('consent-main').checked;
    if (!mainConsent) {
      this.toast('Participation consent is mandatory to proceed.', 'error');
      return;
    }

    this.activeSurvey.consentGiven = mainConsent;
    this.activeSurvey.audioConsent = document.getElementById('consent-audio').checked;
    this.activeSurvey.geoLocationConsent = document.getElementById('consent-geo').checked;
    this.activeSurvey.participantCanWithdraw = document.getElementById('consent-withdraw').checked;
    this.activeSurvey.hasSignature = this.hasSignature;
    this.activeSurvey.consentTimestamp = Date.now();

    await DB.saveSurvey(this.activeSurvey);

    // Decide initial phase
    if (this.currentInstrument.sections && this.currentInstrument.sections.length > 0) {
      this.showingThemes = false;
      this.currentSectionIndex = 0;
      this.currentQuestionIndex = 0;
    } else {
      this.showingThemes = true;
      this.currentThemeIndex = 0;
    }

    this.navigate('runner');
  }

  // 6. Survey Questionnaire & Theme Runner
  renderRunnerView() {
    const inst = this.currentInstrument;
    if (!inst) return '<div>No active instrument</div>';

    if (this.showingThemes) {
      return this.renderThemeRunnerView();
    } else {
      return this.renderQuestionRunnerView();
    }
  }

  renderQuestionRunnerView() {
    const inst = this.currentInstrument;
    const section = inst.sections[this.currentSectionIndex];
    const question = section.questions[this.currentQuestionIndex];

    // Compute progress
    let totalQuestions = 0;
    let answeredQuestions = 0;
    inst.sections.forEach(s => {
      s.questions.forEach(q => {
        totalQuestions++;
        if (this.surveyAnswers[q.id] !== undefined && this.surveyAnswers[q.id] !== '') {
          answeredQuestions++;
        }
      });
    });
    const progressPct = Math.round((answeredQuestions / totalQuestions) * 100);

    return `
      <div class="fade-in" style="max-width: 760px; margin: 0 auto;">
        <!-- Header & Breadcrumbs -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
          <span class="badge badge-primary">Section ${section.code}: ${section.title}</span>
          <button class="btn btn-outline btn-sm" onclick="app.saveDraftAndReturnHome()">Save Draft & Exit</button>
        </div>

        <!-- Progress Bar -->
        <div class="progress-bar-container">
          <div class="progress-bar-fill" style="width: ${progressPct}%;"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 0.75rem; color: var(--text-subtle); margin-bottom: 1.25rem;">
          <span>Question ${this.currentQuestionIndex + 1} of ${section.questions.length} (Section ${section.code})</span>
          <span>${progressPct}% Completed</span>
        </div>

        <!-- Question Card -->
        <div class="card" style="padding: 2rem;">
          <div style="font-family: var(--font-mono); font-size: 0.85rem; color: var(--primary); font-weight: 700; margin-bottom: 0.5rem;">
            [${question.id}]
          </div>
          <h2 style="font-size: 1.25rem; font-weight: 700; line-height: 1.45; margin-bottom: 1.5rem;">
            ${question.text}
          </h2>

          <div id="question-input-area">
            ${this.renderQuestionInput(question)}
          </div>
        </div>

        <!-- Navigation Buttons -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 1.5rem;">
          <button type="button" class="btn btn-outline" onclick="app.runnerPreviousQuestion()" ${this.isFirstQuestion() ? 'disabled' : ''}>
            ← Previous Question
          </button>
          <button type="button" class="btn btn-primary btn-lg" onclick="app.runnerNextQuestion()">
            ${this.isLastSectionQuestion() && !inst.themes ? 'Review Survey Summary →' : 'Next Step →'}
          </button>
        </div>
      </div>
    `;
  }

  renderQuestionInput(question) {
    const currentVal = this.surveyAnswers[question.id] || '';

    switch (question.inputType) {
      case 'NUMBER':
        return `
          <input type="number" class="form-control" id="input-${question.id}" value="${currentVal}" 
            placeholder="Enter numerical response..." onchange="app.recordAnswer('${question.id}', this.value)" style="font-size: 1.2rem; max-width: 250px;">
        `;
      case 'TEXT':
        return `
          <input type="text" class="form-control" id="input-${question.id}" value="${currentVal}" 
            placeholder="Enter response text..." onchange="app.recordAnswer('${question.id}', this.value)">
        `;
      case 'FREE_TEXT':
        return `
          <textarea class="form-control" id="input-${question.id}" rows="4" 
            placeholder="Record detailed field observation..." onchange="app.recordAnswer('${question.id}', this.value)">${currentVal}</textarea>
        `;
      case 'SINGLE_CHOICE':
        return `
          <div class="choice-grid">
            ${(question.options || []).map(opt => `
              <div class="choice-tile ${currentVal === opt ? 'selected' : ''}" onclick="app.selectSingleChoice('${question.id}', '${opt.replace(/'/g, "\\'")}')">
                <div class="choice-indicator"></div>
                <span style="font-size: 0.88rem; font-weight: 500;">${opt}</span>
              </div>
            `).join('')}
          </div>
        `;
      case 'MULTI_CHOICE':
        const selectedList = Array.isArray(currentVal) ? currentVal : (currentVal ? currentVal.split(', ') : []);
        return `
          <div class="choice-grid">
            ${(question.options || []).map(opt => {
              const isChecked = selectedList.includes(opt);
              return `
                <div class="choice-tile ${isChecked ? 'selected' : ''}" onclick="app.toggleMultiChoice('${question.id}', '${opt.replace(/'/g, "\\'")}')">
                  <div class="choice-indicator" style="border-radius: 4px;">${isChecked ? '✓' : ''}</div>
                  <span style="font-size: 0.88rem; font-weight: 500;">${opt}</span>
                </div>
              `;
            }).join('')}
          </div>
        `;
      case 'LIKERT':
        return `
          <div class="likert-scale">
            ${(question.options || ["Strongly Disagree", "Disagree", "Neither", "Agree", "Strongly Agree"]).map((opt, idx) => `
              <div class="likert-btn ${currentVal === opt ? 'selected' : ''}" onclick="app.selectSingleChoice('${question.id}', '${opt}')">
                <span class="num">${idx + 1}</span>
                <span>${opt}</span>
              </div>
            `).join('')}
          </div>
        `;
      case 'TABLE':
        return this.renderServiceTableGrid(question);
      default:
        return `<input type="text" class="form-control" onchange="app.recordAnswer('${question.id}', this.value)" value="${currentVal}">`;
    }
  }

  renderServiceTableGrid(question) {
    const city = this.activeSurvey.city || 'ND';
    const services = CONFIG.CITY_SERVICES[city] || CONFIG.CITY_SERVICES['ND'];

    return `
      <div style="overflow-x: auto;">
        <table class="service-table">
          <thead>
            <tr>
              <th>Smart City Service</th>
              <th style="text-align:center;">Heard of it?</th>
              <th style="text-align:center;">Tried it?</th>
              <th style="text-align:center;">Used Alone</th>
              <th style="text-align:center;">With Help</th>
              <th style="text-align:center;">Never/Gave Up</th>
            </tr>
          </thead>
          <tbody>
            ${services.map(service => {
              const sKey = `C_GRID_${service}`;
              const heard = this.surveyAnswers[`${sKey}_heard`] || 'No';
              const tried = this.surveyAnswers[`${sKey}_tried`] || 'No';
              const alone = this.surveyAnswers[`${sKey}_alone`] || 'No';
              const help = this.surveyAnswers[`${sKey}_help`] || 'No';
              const never = this.surveyAnswers[`${sKey}_never`] || 'No';

              return `
                <tr>
                  <td style="font-weight: 600;">${service}</td>
                  <td style="text-align:center;">
                    <input type="checkbox" ${heard === 'Yes' ? 'checked' : ''} onchange="app.recordAnswer('${sKey}_heard', this.checked ? 'Yes' : 'No')">
                  </td>
                  <td style="text-align:center;">
                    <input type="checkbox" ${tried === 'Yes' ? 'checked' : ''} onchange="app.recordAnswer('${sKey}_tried', this.checked ? 'Yes' : 'No')">
                  </td>
                  <td style="text-align:center;">
                    <input type="checkbox" ${alone === 'Yes' ? 'checked' : ''} onchange="app.recordAnswer('${sKey}_alone', this.checked ? 'Yes' : 'No')">
                  </td>
                  <td style="text-align:center;">
                    <input type="checkbox" ${help === 'Yes' ? 'checked' : ''} onchange="app.recordAnswer('${sKey}_help', this.checked ? 'Yes' : 'No')">
                  </td>
                  <td style="text-align:center;">
                    <input type="checkbox" ${never === 'Yes' ? 'checked' : ''} onchange="app.recordAnswer('${sKey}_never', this.checked ? 'Yes' : 'No')">
                  </td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    `;
  }

  // Qualitative Theme Runner View
  renderThemeRunnerView() {
    const inst = this.currentInstrument;
    const theme = inst.themes[this.currentThemeIndex];
    const themeKey = `theme_${theme.code}`;
    const mainResponse = this.surveyAnswers[themeKey] || '';

    // City probes
    const cityProbes = (theme.citySpecificProbes && theme.citySpecificProbes[this.activeSurvey.city]) || [];
    const allProbes = [...theme.probes, ...cityProbes];

    return `
      <div class="fade-in" style="max-width: 800px; margin: 0 auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
          <span class="badge badge-sky">Qualitative Theme ${theme.code} (${this.currentThemeIndex + 1} of ${inst.themes.length})</span>
          <button class="btn btn-outline btn-sm" onclick="app.saveDraftAndReturnHome()">Save Draft & Exit</button>
        </div>

        <h1 style="font-family: var(--font-display); font-size: 1.45rem; font-weight: 700; margin-bottom: 1.25rem;">
          Theme ${theme.code}: ${theme.title}
        </h1>

        <!-- Opening Question Card -->
        <div class="card" style="border-left: 4px solid var(--primary);">
          <span style="font-size: 0.75rem; font-weight: 700; color: #818cf8; text-transform: uppercase;">Opening Narrative Question</span>
          <p style="font-size: 1.15rem; font-weight: 600; line-height: 1.5; margin: 0.75rem 0 1.25rem;">
            "${theme.openingQuestion}"
          </p>

          <!-- Voice Dictation Helper Bar -->
          <div class="voice-recorder-bar">
            <div class="voice-status">
              <span id="voice-indicator" class="${this.isRecordingVoice ? 'pulse-dot' : ''}"></span>
              <span id="voice-status-text">${this.isRecordingVoice ? 'Listening & Transcribing...' : 'Voice Dictation'}</span>
            </div>
            <button type="button" class="btn ${this.isRecordingVoice ? 'btn-rose' : 'btn-primary'} btn-sm" onclick="app.toggleVoiceRecording('${themeKey}')">
              ${this.isRecordingVoice ? '⏹ Stop Dictation' : '🎙️ Start Dictation'}
            </button>
          </div>

          <textarea id="input-${themeKey}" class="form-control" rows="5" placeholder="Record narrative response and verbatim transcript..."
            oninput="app.recordAnswer('${themeKey}', this.value)">${mainResponse}</textarea>
        </div>

        <!-- Follow-up Probes Card -->
        <div class="card">
          <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.5rem; color: #34d399;">Follow-Up Inquiry Probes</h3>
          <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 1rem;">
            Select probes actually explored during the interview and record specific observations:
          </p>

          <div style="display: flex; flex-direction: column; gap: 1rem;">
            ${allProbes.map((probe, pIdx) => {
              const probeKey = `probe_${theme.code}_${pIdx}`;
              const isSelected = !!this.selectedProbes[probeKey];
              const probeResp = this.probeResponses[probeKey] || '';

              return `
                <div style="background: rgba(15, 23, 42, 0.4); border: 1px solid var(--border-glass); border-radius: var(--radius-md); padding: 1rem;">
                  <label style="display: flex; gap: 0.75rem; align-items: flex-start; cursor: pointer; user-select: none;">
                    <input type="checkbox" ${isSelected ? 'checked' : ''} onchange="app.toggleProbeSelection('${probeKey}', this.checked)" style="margin-top: 0.25rem; accent-color: var(--primary);">
                    <span style="font-size: 0.9rem; font-weight: 500; line-height: 1.4;">${probe}</span>
                  </label>
                  ${isSelected ? `
                    <div style="margin-top: 0.75rem; padding-left: 1.75rem;">
                      <textarea class="form-control" rows="2" placeholder="Probe response / notes..." 
                        oninput="app.recordProbeResponse('${probeKey}', this.value)">${probeResp}</textarea>
                    </div>
                  ` : ''}
                </div>
              `;
            }).join('')}
          </div>
        </div>

        <!-- Navigation Buttons -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 1.5rem;">
          <button type="button" class="btn btn-outline" onclick="app.runnerPreviousTheme()">
            ← Previous Theme
          </button>
          <button type="button" class="btn btn-primary btn-lg" onclick="app.runnerNextTheme()">
            ${this.currentThemeIndex < inst.themes.length - 1 ? 'Next Theme →' : 'Review Survey Summary →'}
          </button>
        </div>
      </div>
    `;
  }

  // Answer recording helpers
  recordAnswer(qId, val) {
    this.surveyAnswers[qId] = val;
    this.saveActiveSurveyDraft();
  }

  selectSingleChoice(qId, val) {
    this.recordAnswer(qId, val);
    this.navigate('runner');
  }

  toggleMultiChoice(qId, val) {
    const current = this.surveyAnswers[qId] || '';
    let list = current ? current.split(', ') : [];
    if (list.includes(val)) {
      list = list.filter(item => item !== val);
    } else {
      list.push(val);
    }
    this.recordAnswer(qId, list.join(', '));
    this.navigate('runner');
  }

  toggleProbeSelection(probeKey, isChecked) {
    this.selectedProbes[probeKey] = isChecked;
    this.saveActiveSurveyDraft();
    this.navigate('runner');
  }

  recordProbeResponse(probeKey, val) {
    this.probeResponses[probeKey] = val;
    this.saveActiveSurveyDraft();
  }

  // Skip logic helper
  shouldShowQuestion(qId) {
    if (qId === 'C12') {
      const c11 = this.surveyAnswers['C11'] || '';
      return c11.startsWith('Yes');
    }
    if (qId === 'E6') {
      const e5 = this.surveyAnswers['E5'] || '';
      return e5.includes('more dependent');
    }
    if (qId === 'G2') {
      const g1 = this.surveyAnswers['G1'] || '';
      return g1.startsWith('Yes');
    }
    return true;
  }

  runnerNextQuestion() {
    const inst = this.currentInstrument;
    const section = inst.sections[this.currentSectionIndex];

    // Find next valid question in section
    let nextQIndex = this.currentQuestionIndex + 1;
    while (nextQIndex < section.questions.length) {
      if (this.shouldShowQuestion(section.questions[nextQIndex].id)) {
        this.currentQuestionIndex = nextQIndex;
        this.navigate('runner');
        return;
      }
      nextQIndex++;
    }

    // Next section
    if (this.currentSectionIndex < inst.sections.length - 1) {
      this.currentSectionIndex++;
      this.currentQuestionIndex = 0;
      this.navigate('runner');
      return;
    }

    // Switch to qualitative themes phase if available
    if (inst.themes && inst.themes.length > 0) {
      this.showingThemes = true;
      this.currentThemeIndex = 0;
      this.navigate('runner');
      return;
    }

    // Complete questions -> go to summary
    this.navigate('summary');
  }

  runnerPreviousQuestion() {
    const inst = this.currentInstrument;
    if (this.currentQuestionIndex > 0) {
      let prevQ = this.currentQuestionIndex - 1;
      while (prevQ >= 0) {
        if (this.shouldShowQuestion(inst.sections[this.currentSectionIndex].questions[prevQ].id)) {
          this.currentQuestionIndex = prevQ;
          this.navigate('runner');
          return;
        }
        prevQ--;
      }
    }

    if (this.currentSectionIndex > 0) {
      this.currentSectionIndex--;
      this.currentQuestionIndex = inst.sections[this.currentSectionIndex].questions.length - 1;
      this.navigate('runner');
    }
  }

  runnerNextTheme() {
    const inst = this.currentInstrument;
    if (this.currentThemeIndex < inst.themes.length - 1) {
      this.currentThemeIndex++;
      this.navigate('runner');
    } else {
      this.navigate('summary');
    }
  }

  runnerPreviousTheme() {
    const inst = this.currentInstrument;
    if (this.currentThemeIndex > 0) {
      this.currentThemeIndex--;
      this.navigate('runner');
    } else if (inst.sections && inst.sections.length > 0) {
      this.showingThemes = false;
      this.currentSectionIndex = inst.sections.length - 1;
      this.currentQuestionIndex = inst.sections[this.currentSectionIndex].questions.length - 1;
      this.navigate('runner');
    }
  }

  isFirstQuestion() {
    return this.currentSectionIndex === 0 && this.currentQuestionIndex === 0;
  }

  isLastSectionQuestion() {
    const inst = this.currentInstrument;
    if (!inst.sections) return true;
    return this.currentSectionIndex === inst.sections.length - 1 &&
      this.currentQuestionIndex >= inst.sections[this.currentSectionIndex].questions.length - 1;
  }

  bindRunnerEvents() {}

  // Voice Dictation (Speech-to-Text API)
  initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      this.speechRecognizer = new SpeechRecognition();
      this.speechRecognizer.continuous = true;
      this.speechRecognizer.interimResults = true;
      this.speechRecognizer.lang = 'en-IN'; // Default to Indian English, compatible with Hindi accents
    }
  }

  toggleVoiceRecording(targetInputKey) {
    if (!this.speechRecognizer) {
      this.toast('Speech recognition not supported in this browser. Please type responses manually.', 'warning');
      return;
    }

    if (this.isRecordingVoice) {
      this.speechRecognizer.stop();
      this.isRecordingVoice = false;
      this.toast('Voice dictation stopped', 'info');
      this.navigate('runner');
    } else {
      const inputEl = document.getElementById(`input-${targetInputKey}`);
      this.isRecordingVoice = true;

      this.speechRecognizer.onresult = (event) => {
        let transcript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          transcript += event.results[i][0].transcript;
        }
        if (inputEl) {
          inputEl.value = (inputEl.value ? inputEl.value + ' ' : '') + transcript;
          this.recordAnswer(targetInputKey, inputEl.value);
        }
      };

      this.speechRecognizer.onerror = (e) => {
        console.warn('Speech error:', e.error);
        this.isRecordingVoice = false;
        this.toast(`Dictation notice: ${e.error}`, 'warning');
        this.navigate('runner');
      };

      try {
        this.speechRecognizer.start();
        this.toast('Listening... Speak now', 'info');
        this.navigate('runner');
      } catch (ex) {
        console.error('Failed to start speech recognizer:', ex);
      }
    }
  }

  // 7. Survey Summary & Final Submission View
  renderSummaryView() {
    const inst = this.currentInstrument;
    const survey = this.activeSurvey;
    if (!survey || !inst) return '<div>No active survey</div>';

    return `
      <div class="fade-in" style="max-width: 800px; margin: 0 auto;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
          <div>
            <span class="badge badge-emerald">Final Verification</span>
            <h1 style="font-family: var(--font-display); font-size: 1.75rem; font-weight: 700;">
              Survey Session Summary
            </h1>
            <p style="color: var(--text-muted); font-size: 0.88rem;">UID: ${survey.surveyUid}</p>
          </div>
        </div>

        <!-- Metadata Card -->
        <div class="card" style="margin-bottom: 1.25rem;">
          <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.85rem; color: #818cf8;">Session Metadata</h3>
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.75rem; font-size: 0.88rem;">
            <div><strong>Participant:</strong> ${survey.participantName} (Age: ${survey.participantAge})</div>
            <div><strong>City / Locality:</strong> ${survey.city} - ${survey.manualLocality}</div>
            <div><strong>Surveyor:</strong> ${survey.surveyorId} (${survey.surveyorName})</div>
            <div><strong>GPS Accuracy:</strong> ±${Math.round(survey.gpsAccuracy)}m (${survey.latitude}, ${survey.longitude})</div>
            <div><strong>Consent:</strong> ${survey.consentGiven ? '✓ Consented & Signed' : 'Declined'}</div>
            <div><strong>Instrument:</strong> ${inst.code}</div>
          </div>
        </div>

        <!-- Field Summary & Urgency -->
        <div class="card" style="margin-bottom: 1.25rem;">
          <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.75rem; color: #fbbf24;">Field Summary & Observations</h3>
          <div class="form-group">
            <label class="form-label" for="summary-notes">Field Investigator Qualitative Summary</label>
            <textarea id="summary-notes" class="form-control" rows="3" placeholder="Provide overall qualitative remarks, contextual setting, or critical insights..."
              oninput="app.activeSurvey.manualSummary = this.value">${survey.manualSummary || ''}</textarea>
          </div>
        </div>

        <!-- Responses Preview -->
        <div class="card" style="margin-bottom: 1.5rem;">
          <h3 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 0.75rem;">Recorded Responses</h3>
          <div style="max-height: 320px; overflow-y: auto; background: rgba(0,0,0,0.2); border-radius: var(--radius-md); padding: 1rem; font-size: 0.85rem;">
            ${Object.keys(this.surveyAnswers).length === 0 ? '<p style="color:var(--text-muted);">No responses recorded.</p>' : ''}
            ${Object.entries(this.surveyAnswers).map(([k, v]) => `
              <div style="margin-bottom: 0.65rem; border-bottom: 1px solid var(--border-glass); padding-bottom: 0.45rem;">
                <span style="font-weight: 700; color: #818cf8;">${k}:</span>
                <span style="margin-left: 0.5rem; word-break: break-word;">${v}</span>
              </div>
            `).join('')}
          </div>
        </div>

        <!-- Final Action Bar -->
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
          <button type="button" class="btn btn-outline" onclick="app.navigate('runner')">← Back to Questions</button>
          <div style="display: flex; gap: 0.75rem;">
            <button type="button" class="btn btn-secondary" onclick="app.saveDraftAndReturnHome()">Save as Draft</button>
            <button type="button" class="btn btn-emerald btn-lg" onclick="app.finalizeAndSubmitSurvey()">
              ✓ Submit for Master Approval
            </button>
          </div>
        </div>
      </div>
    `;
  }

  bindSummaryEvents() {}

  async saveActiveSurveyDraft() {
    if (!this.activeSurvey) return;
    this.activeSurvey.answersJson = JSON.stringify(this.surveyAnswers);
    this.activeSurvey.transcriptText = this.compileTranscript();
    this.activeSurvey.updatedAt = Date.now();
    await DB.saveSurvey(this.activeSurvey);
  }

  compileTranscript() {
    let lines = [];
    if (this.currentInstrument && this.currentInstrument.themes) {
      this.currentInstrument.themes.forEach(theme => {
        const key = `theme_${theme.code}`;
        const ans = this.surveyAnswers[key];
        if (ans) {
          lines.push(`Theme ${theme.code}: ${theme.title}`);
          lines.push(`Q: ${theme.openingQuestion}`);
          lines.push(`A: ${ans}\n`);
        }
      });
    }
    return lines.join('\n');
  }

  async saveDraftAndReturnHome() {
    await this.saveActiveSurveyDraft();
    this.toast('Draft saved safely to local storage', 'info');
    this.activeSurvey = null;
    this.navigate('dashboard');
  }

  async finalizeAndSubmitSurvey() {
    await this.saveActiveSurveyDraft();
    this.activeSurvey.manualSummary = (document.getElementById('summary-notes') ? document.getElementById('summary-notes').value : this.activeSurvey.manualSummary) || '';
    this.activeSurvey.status = 'submitted';

    this.toast('Submitting survey to cloud...', 'info');
    const res = await API.submitSurvey(this.activeSurvey);

    if (res.success) {
      this.toast('Survey submitted successfully! Awaiting Master review.', 'success');
    } else {
      this.toast('Submitted locally. Will sync when internet is connected.', 'warning');
    }

    this.activeSurvey = null;
    this.navigate('records');
  }

  // 8. Records Board & Master Review View
  renderRecordsView() {
    const isMaster = this.currentUser && this.currentUser.role === CONFIG.ROLE_MASTER;
    return `
      <div class="fade-in">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
          <div>
            <h1 style="font-family: var(--font-display); font-size: 1.75rem; font-weight: 800;">
              ${isMaster ? 'Master Quality Assurance & Approval Board' : 'Survey Records & Submissions'}
            </h1>
            <p style="color: var(--text-muted); font-size: 0.88rem;">
              ${isMaster ? 'Inspect submissions and execute Double-Doc synchronization.' : 'Review submitted records, drafts, and approval status.'}
            </p>
          </div>
          <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
            ${isMaster ? `
              <button class="btn btn-primary btn-sm" onclick="app.syncCloudRecordsLive()">
                🔄 Live Cloud Sync
              </button>
            ` : ''}
            <button class="btn btn-outline btn-sm" onclick="app.exportRecordsTsv()">Export TSV</button>
            <button class="btn btn-outline btn-sm" onclick="app.exportRecordsJson()">Export JSON</button>
          </div>
        </div>

        <!-- Status Filter Tabs -->
        <div style="display: flex; gap: 0.5rem; overflow-x: auto; padding-bottom: 0.5rem; margin-bottom: 1.25rem;">
          <button class="btn btn-sm btn-primary records-tab" data-filter="ALL" onclick="app.filterRecords('ALL')">All Records</button>
          <button class="btn btn-sm btn-outline records-tab" data-filter="submitted" onclick="app.filterRecords('submitted')">Pending Review</button>
          <button class="btn btn-sm btn-outline records-tab" data-filter="approved" onclick="app.filterRecords('approved')">Approved</button>
          <button class="btn btn-sm btn-outline records-tab" data-filter="DRAFT" onclick="app.filterRecords('DRAFT')">Local Drafts</button>
        </div>

        <!-- Search Bar -->
        <div class="form-group" style="margin-bottom: 1.5rem;">
          <input type="text" id="record-search" class="form-control" placeholder="Search by participant name, surveyor ID, or UID..."
            oninput="app.searchRecords(this.value)">
        </div>

        <!-- Records List Container -->
        <div id="records-list-container" style="display: flex; flex-direction: column; gap: 1rem;">
          <p style="color: var(--text-muted);">Loading records...</p>
        </div>
      </div>
    `;
  }

  async bindRecordsEvents() {
    this.filterRecords('ALL');
  }

  async filterRecords(filterStatus) {
    document.querySelectorAll('.records-tab').forEach(tab => {
      if (tab.getAttribute('data-filter') === filterStatus) {
        tab.classList.add('btn-primary');
        tab.classList.remove('btn-outline');
      } else {
        tab.classList.remove('btn-primary');
        tab.classList.add('btn-outline');
      }
    });

    const allSurveys = await DB.getAllSurveys();
    const query = (document.getElementById('record-search') ? document.getElementById('record-search').value : '').toLowerCase();

    let filtered = allSurveys;
    if (filterStatus === 'submitted') {
      filtered = filtered.filter(s => s.status === 'submitted' || s.status === 'COMPLETED');
    } else if (filterStatus !== 'ALL') {
      filtered = filtered.filter(s => s.status === filterStatus);
    }

    if (query) {
      filtered = filtered.filter(s => 
        (s.participantName || '').toLowerCase().includes(query) ||
        (s.surveyUid || '').toLowerCase().includes(query) ||
        (s.surveyorId || '').toLowerCase().includes(query)
      );
    }

    this.renderRecordsList(filtered);
  }

  searchRecords(query) {
    const activeTab = document.querySelector('.records-tab.btn-primary');
    const filter = activeTab ? activeTab.getAttribute('data-filter') : 'ALL';
    this.filterRecords(filter);
  }

  renderRecordsList(surveys) {
    const container = document.getElementById('records-list-container');
    if (!container) return;

    if (surveys.length === 0) {
      container.innerHTML = `
        <div class="card" style="text-align: center; padding: 3rem 1rem;">
          <p style="font-size: 1.1rem; color: var(--text-muted); margin-bottom: 0.5rem;">No survey records found.</p>
          <span style="font-size: 0.85rem; color: var(--text-subtle);">Start a new survey or click Live Cloud Sync to pull remote submissions.</span>
        </div>
      `;
      return;
    }

    const isMaster = this.currentUser && this.currentUser.role === CONFIG.ROLE_MASTER;

    container.innerHTML = surveys.map(s => {
      const isApproved = s.status === 'approved';
      const isPending = s.status === 'submitted' || s.status === 'COMPLETED';
      const isDraft = s.status === 'DRAFT';
      const dateStr = new Date(s.createdAt || Date.now()).toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
      });

      return `
        <div class="card" style="padding: 1.25rem;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 0.5rem;">
            <div>
              <div style="display: flex; align-items: center; gap: 0.65rem;">
                <h3 style="font-size: 1.1rem; font-weight: 700;">${s.participantName || 'Anonymous Participant'}</h3>
                <span class="badge ${isApproved ? 'badge-emerald' : isPending ? 'badge-amber' : 'badge-primary'}">
                  ${s.status.toUpperCase()}
                </span>
              </div>
              <span style="font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-subtle);">${s.surveyUid}</span>
            </div>
            <div style="text-align: right; font-size: 0.8rem; color: var(--text-muted);">
              <div>${dateStr}</div>
              <div>Surveyor: <strong>${s.surveyorId}</strong></div>
            </div>
          </div>

          <div style="display: flex; gap: 1.5rem; font-size: 0.82rem; color: var(--text-muted); margin-bottom: 1rem; flex-wrap: wrap;">
            <span>Instrument: <strong>${s.instrumentType}</strong></span>
            <span>City: <strong>${s.city} (${s.manualLocality || 'N/A'})</strong></span>
            <span>GPS: <strong>±${Math.round(s.gpsAccuracy || 0)}m</strong></span>
          </div>

          <!-- Actions Bar -->
          <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-glass); padding-top: 0.85rem; flex-wrap: wrap; gap: 0.75rem;">
            <button class="btn btn-outline btn-sm" onclick="app.inspectRecord('${s.surveyUid}')">
              👁️ View Record Details
            </button>

            <div style="display: flex; gap: 0.5rem;">
              ${isDraft ? `
                <button class="btn btn-primary btn-sm" onclick="app.resumeDraft('${s.surveyUid}')">Resume Draft</button>
                <button class="btn btn-outline btn-sm" onclick="app.deleteSurveyRecord('${s.surveyUid}')">Delete</button>
              ` : ''}

              ${isMaster && isPending ? `
                <button class="btn btn-emerald btn-sm" onclick="app.approveRecord('${s.surveyUid}')">
                  ✓ Approve & Push to Docs
                </button>
                <button class="btn btn-rose btn-sm" onclick="app.showRejectModal('${s.surveyUid}')">
                  Reject
                </button>
              ` : ''}

              ${isApproved ? `
                <a href="${CONFIG.getDocUrlForSurveyor(s.surveyorId)}" target="_blank" rel="noopener noreferrer" class="btn btn-emerald btn-sm">
                  📄 Open in Google Doc
                </a>
              ` : ''}
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  // Master Approval Action (Executes Double-Doc Engine)
  async approveRecord(surveyUid) {
    const survey = await DB.getSurvey(surveyUid);
    if (!survey) return;

    const confirmApprove = confirm(`Execute Master Approval for record ${surveyUid}?\n\n- Full Survey will be routed to Field Doc (${survey.surveyorId})\n- Official Approval Audit will be appended to Central Master Doc`);
    if (!confirmApprove) return;

    this.toast('Executing Double-Doc Cloud Push...', 'info');
    const result = await API.approveSurvey(surveyUid, 'Approved by Master QA', this.currentUser.name || 'Master Administrator');

    if (result.success) {
      this.toast('Approved! Record pushed to dedicated Google Doc.', 'success');
      // Automatically prompt to open document
      if (result.docUrl) {
        const openDoc = confirm('Approval successful! Open the Field Investigator Google Document now?');
        if (openDoc) {
          window.open(result.docUrl, '_blank');
        }
      }
    } else {
      this.toast(`Approval completed locally: ${result.error || 'Check internet'}`, 'warning');
    }

    this.filterRecords('ALL');
  }

  showRejectModal(surveyUid) {
    const reason = prompt('Please enter Quality Assurance rejection notes or required corrections:');
    if (reason === null) return;
    if (!reason.trim()) {
      this.toast('Rejection notes are required', 'error');
      return;
    }

    API.rejectSurvey(surveyUid, reason.trim()).then(() => {
      this.toast('Survey rejected and notes recorded', 'info');
      this.filterRecords('ALL');
    });
  }

  async deleteSurveyRecord(surveyUid) {
    const confirmDelete = confirm('Are you sure you want to delete this draft?');
    if (!confirmDelete) return;
    await DB.deleteSurvey(surveyUid);
    this.toast('Draft deleted', 'info');
    this.filterRecords('ALL');
  }

  async resumeDraft(surveyUid) {
    const survey = await DB.getSurvey(surveyUid);
    if (!survey) return;
    this.activeSurvey = survey;
    this.currentInstrument = INSTRUMENT_BANK.getInstrument(survey.instrumentType);
    try {
      this.surveyAnswers = JSON.parse(survey.answersJson || '{}');
    } catch (e) {
      this.surveyAnswers = {};
    }
    this.currentSectionIndex = 0;
    this.currentQuestionIndex = 0;
    this.showingThemes = false;
    this.navigate('runner');
  }

  async inspectRecord(surveyUid) {
    const survey = await DB.getSurvey(surveyUid);
    if (!survey) return;

    let answersMap = {};
    try {
      answersMap = JSON.parse(survey.answersJson || '{}');
    } catch (e) {
      answersMap = { raw: survey.answersJson };
    }

    const modalHtml = `
      <div class="modal-overlay" id="inspect-modal" onclick="if(event.target.id === 'inspect-modal') app.closeModal()">
        <div class="modal-container">
          <div class="modal-header">
            <div>
              <h3 style="font-size: 1.15rem; font-weight: 700;">${survey.participantName || 'Participant'}</h3>
              <span style="font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-subtle);">${survey.surveyUid}</span>
            </div>
            <button class="btn btn-outline btn-icon" onclick="app.closeModal()">✕</button>
          </div>
          <div class="modal-body">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; font-size: 0.85rem; margin-bottom: 1.25rem;">
              <div><strong>Status:</strong> <span class="badge badge-primary">${survey.status}</span></div>
              <div><strong>Surveyor:</strong> ${survey.surveyorId} (${survey.surveyorName})</div>
              <div><strong>City:</strong> ${survey.city} - ${survey.manualLocality}</div>
              <div><strong>GPS:</strong> ±${Math.round(survey.gpsAccuracy || 0)}m (${survey.latitude}, ${survey.longitude})</div>
              ${survey.qaNotes ? `<div style="grid-column: 1 / -1;"><strong>QA Notes:</strong> ${survey.qaNotes}</div>` : ''}
              ${survey.approvedBy ? `<div style="grid-column: 1 / -1;"><strong>Approved By:</strong> ${survey.approvedBy}</div>` : ''}
            </div>

            <h4 style="font-size: 0.95rem; font-weight: 700; color: #818cf8; margin-bottom: 0.5rem;">Survey Q&A Responses</h4>
            <div style="background: rgba(0,0,0,0.25); border-radius: var(--radius-md); padding: 1rem; max-height: 250px; overflow-y: auto; font-size: 0.85rem; margin-bottom: 1rem;">
              ${Object.entries(answersMap).map(([k, v]) => `
                <div style="margin-bottom: 0.5rem; border-bottom: 1px solid var(--border-glass); padding-bottom: 0.35rem;">
                  <strong style="color: #93c5fd;">${k}:</strong><br>
                  <span>${v}</span>
                </div>
              `).join('')}
            </div>

            ${survey.transcriptText ? `
              <h4 style="font-size: 0.95rem; font-weight: 700; color: #34d399; margin-bottom: 0.5rem;">Transcript / Notes</h4>
              <div style="background: rgba(0,0,0,0.25); border-radius: var(--radius-md); padding: 1rem; font-size: 0.85rem; white-space: pre-wrap; margin-bottom: 1rem;">
                ${survey.transcriptText}
              </div>
            ` : ''}

            ${survey.manualSummary ? `
              <h4 style="font-size: 0.95rem; font-weight: 700; color: #fbbf24; margin-bottom: 0.5rem;">Field Summary</h4>
              <p style="font-size: 0.85rem; color: var(--text-main);">${survey.manualSummary}</p>
            ` : ''}
          </div>
          <div class="modal-footer">
            <a href="${CONFIG.getDocUrlForSurveyor(survey.surveyorId)}" target="_blank" rel="noopener noreferrer" class="btn btn-outline btn-sm">
              📄 View Document
            </a>
            <button class="btn btn-primary btn-sm" onclick="app.closeModal()">Close</button>
          </div>
        </div>
      </div>
    `;

    const existingModal = document.getElementById('inspect-modal');
    if (existingModal) existingModal.remove();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
  }

  closeModal() {
    const modal = document.getElementById('inspect-modal');
    if (modal) modal.remove();
    const switchModal = document.getElementById('master-switch-modal');
    if (switchModal) switchModal.remove();
  }

  showMasterSwitchModal() {
    const modalHtml = `
      <div class="modal-overlay" id="master-switch-modal" onclick="if(event.target.id === 'master-switch-modal') app.closeModal()">
        <div class="modal-container" style="max-width: 440px;">
          <div class="modal-header">
            <h3 style="font-size: 1.15rem; font-weight: 700;">🔒 Switch to Master Role</h3>
            <button class="btn btn-outline btn-icon" onclick="app.closeModal()">✕</button>
          </div>
          <div class="modal-body">
            <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 1rem;">
              Enter the Master authorization password to activate full review and Google Doc approval rights. Session lasts 5 minutes.
            </p>
            <div class="form-group">
              <label class="form-label" for="master-pass-input">Master Password</label>
              <input type="password" id="master-pass-input" class="form-control" placeholder="Enter master password" autofocus>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline btn-sm" onclick="app.closeModal()">Cancel</button>
            <button class="btn btn-rose btn-sm" onclick="app.submitMasterSwitch()">Activate Master</button>
          </div>
        </div>
      </div>
    `;
    const existing = document.getElementById('master-switch-modal');
    if (existing) existing.remove();
    document.body.insertAdjacentHTML('beforeend', modalHtml);
  }

  async submitMasterSwitch() {
    const pass = document.getElementById('master-pass-input').value;
    const success = await this.switchToMasterWithPassword(pass);
    if (success) {
      this.closeModal();
      this.navigate(this.currentView);
    }
  }

  async syncCloudRecordsLive() {
    this.toast('Streaming surveys from cloud tunnel...', 'info');
    const result = await API.fetchCloudRecords();
    if (result.success) {
      this.toast(`Synced ${result.syncedCount} records into local board`, 'success');
      this.filterRecords('ALL');
    } else {
      this.toast(`Sync notice: ${result.error || 'Check internet'}`, 'warning');
    }
  }

  // 9. Export Functions
  async exportRecordsJson() {
    const surveys = await DB.getAllSurveys();
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(surveys, null, 2));
    const a = document.createElement('a');
    a.href = dataStr;
    a.download = `CivicAge_Surveys_${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    this.toast('JSON export downloaded', 'success');
  }

  async exportRecordsTsv() {
    const surveys = await DB.getAllSurveys();
    const headers = ['Record_UID', 'Created_At', 'Surveyor_ID', 'Surveyor_Name', 'Participant_Name', 'Age', 'Gender', 'City', 'Locality', 'Latitude', 'Longitude', 'GPS_Accuracy', 'Status', 'QA_Notes', 'Answers'];
    const rows = surveys.map(s => [
      s.surveyUid || '',
      new Date(s.createdAt || Date.now()).toISOString(),
      s.surveyorId || '',
      s.surveyorName || '',
      s.participantName || '',
      s.participantAge || '',
      s.participantGender || '',
      s.city || '',
      s.manualLocality || '',
      s.latitude || '',
      s.longitude || '',
      s.gpsAccuracy || '',
      s.status || '',
      (s.qaNotes || '').replace(/\t|\n/g, ' '),
      (s.answersJson || '').replace(/\t|\n/g, ' ')
    ]);

    const tsvContent = [headers.join('\t'), ...rows.map(r => r.join('\t'))].join('\n');
    const blob = new Blob([tsvContent], { type: 'text/tab-separated-values;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `CivicAge_Surveys_${new Date().toISOString().slice(0, 10)}.tsv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    this.toast('TSV export downloaded', 'success');
  }

  // Toast Notification System
  toast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const el = document.createElement('div');
    el.className = `toast ${type}`;
    let icon = 'ℹ️';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '✕';
    if (type === 'warning') icon = '⚠️';

    el.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    container.appendChild(el);

    setTimeout(() => {
      el.style.opacity = '0';
      el.style.transform = 'translateY(10px)';
      el.style.transition = 'all 0.25s ease';
      setTimeout(() => el.remove(), 250);
    }, 3500);
  }

  handleNetworkChange(isOnline) {
    if (isOnline) {
      this.toast('Network connected. Cloud sync available.', 'success');
    } else {
      this.toast('Offline mode active. All records saved safely to IndexedDB.', 'warning');
    }
  }

  toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'dark';
    const next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    DB.setSetting('theme', next);
  }
}

// Global instance
const app = new CivicAgeApp();
window.app = app;
document.addEventListener('DOMContentLoaded', () => app.init());

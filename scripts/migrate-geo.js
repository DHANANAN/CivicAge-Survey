/**
 * Migration Script: Geo Tagging Compliance Audit
 * Scans Firestore `instruments` collection, identifies records missing valid geo
 * data (accuracy > 100m or missing timestamp), updates status to `pending_geo`,
 * and generates notification logs for the respective surveyors.
 *
 * Usage:
 *   NODE_ENV=production node scripts/migrate-geo.js
 */

const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
}

const db = admin.firestore();

async function runGeoMigration() {
  console.log('=== DCA Geo Migration Script Started ===');
  const instrumentsRef = db.collection('instruments');
  const snapshot = await instrumentsRef.get();

  let totalScanned = 0;
  let markedPendingGeo = 0;
  let validGeoCount = 0;

  const batch = db.batch();

  for (const doc of snapshot.docs) {
    totalScanned++;
    const data = doc.data();
    const geo = data.geo;

    const isGeoValid =
      geo &&
      typeof geo.lat === 'number' &&
      typeof geo.lng === 'number' &&
      typeof geo.accuracy === 'number' &&
      geo.accuracy <= 100 &&
      geo.timestamp != null;

    if (!isGeoValid && data.status !== 'pending_geo') {
      console.warn(`[Migrate] Instrument ${doc.id} (Owner: ${data.createdBy || 'unknown'}) lacks valid geo. Marking pending_geo...`);
      
      batch.update(doc.ref, {
        status: 'pending_geo',
        geoMigrationNote: 'Marked pending_geo during v2.2 compliance audit',
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Create owner notification
      if (data.createdBy) {
        const notifRef = db.collection('notifications').doc();
        batch.set(notifRef, {
          userId: data.createdBy,
          title: 'Action Required: Geo Location Missing',
          message: `Instrument ${doc.id} requires high-accuracy location re-capture before it can be submitted for QA approval.`,
          instrumentId: doc.id,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
        });
      }

      markedPendingGeo++;
    } else {
      validGeoCount++;
    }
  }

  if (markedPendingGeo > 0) {
    await batch.commit();
    console.log(`[Migrate] Successfully committed changes for ${markedPendingGeo} records.`);
  }

  console.log('\n=== Migration Audit Summary ===');
  console.log(`Total Instruments Scanned: ${totalScanned}`);
  console.log(`Valid Geo Records:        ${validGeoCount}`);
  console.log(`Marked as pending_geo:    ${markedPendingGeo}`);
  console.log('===============================\n');
}

runGeoMigration().catch((err) => {
  console.error('Migration failed:', err);
  process.exit(1);
});

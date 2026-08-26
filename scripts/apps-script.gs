/**
 * DCA Cloud Sync - Google Doc Appender v3.5 (Dual-Tunnel Memory Enabled)
 * URL: https://script.google.com/macros/s/AKfycbziNNzXw3FPAdLiWEF_dbeqo2ThLuGWb-jUQpaHg2cLSt6G1GiCDckLQSXaGYMQa_YxFw/exec
 * Token: DCA_SECRET_TOKEN_2026
 * REDEPLOY: Deploy > Manage > pencil > New Version > Deploy
 * TEST: select testWrite() then Run
 */

const COMMON_DOC_ID = '10U1MCP3_neriSMV47ch7OAD15Ev2FgtQjLW8svfXraQ';
const SECRET_TOKEN  = 'DCA_SECRET_TOKEN_2026';
const USER_DOC_MAP = {
  'SC0101': '1wdTMizfwXXqYpsy5NPxYCug-I7vRACsWZZZ7KmtWF3w',
  'SC0102': '1Munad_OGvWbRuav4VfeK9KKXHVEYBAkAhpo0hRfVGzo',
  'SC0103': '1WINw7vf7QfoufrYBVpM8200xX1R7u2QOIMcByX6yhok',
  'SC0104': '1Lb7_4ZhSGTjIiaonoiCXF6IYd4stNHPYEqCRtHCGZ6M',
  'SC0105': '1wXeElRpfh8scbRIgAGuqxhHt-gM6a_CViqM-05TDhaw',
  'SC0106': '1uay6xdP1luqggV141ywnYy9ibAXIGkPrfCYmu0ENnvY',
  'SC0107': '1QGHiZyEmALLSYfCPbO8smupd3_FKKTFGcx0Tfm8YEzA',
  'SC0108': '1TaWWC9xLOY1pJW4QXSjXOA9Ml89CKzD1VS_THRQAKZk',
  'SC0109': '1F5HJ0rUOunJqOb7dipPyUFSyt2L0lgtjZHFxbtTahxA',
  'SC0110': '1LfTkNS2UEVNGQMkv1FPrp4uuPK3NxOBLFhJuiD64i_M',
  'SC0111': '1rXfwAA5EacdrUr_ygMo4M9-n3ugydPYkkQ3IO_lAyR0',
  'SC0112': '1RkN84SXU1_HH_Cny_KSi4-_lOy5FwmRWp7095KbdgoQ'
};

function doOptions(e){return ContentService.createTextOutput('').setMimeType(ContentService.MimeType.TEXT);}

function doGet(e){
  if (e && e.parameter && e.parameter.action === 'getRecords') {
    var token = e.parameter.token;
    if (token !== SECRET_TOKEN) return json({ success: false, error: 'Unauthorized' });
    var records = getMemoryRecords();
    return json({ success: true, count: records.length, records: records });
  }
  return json({ status: 'DCA v3.5 OK', docs: Object.keys(USER_DOC_MAP).length + ' dedicated + 1 common' });
}

function doPost(e){
  try{
    if(!e||!e.postData||!e.postData.contents)return json({success:false,error:'Empty body'});
    var p=JSON.parse(e.postData.contents);
    if(!p.token||p.token!==SECRET_TOKEN)return json({success:false,error:'Unauthorized'});
    var uid=(p.userId||'').toUpperCase().trim();
    var docId=USER_DOC_MAP[uid]||COMMON_DOC_ID;
    writeToDoc(docId,p);
    saveMemoryRecord(p);
    return json({success:true,docId:docId,recordId:p.recordId||'',routed:uid});
  }catch(ex){return json({success:false,error:ex.toString()});}
}

function saveMemoryRecord(p) {
  try {
    var props = PropertiesService.getScriptProperties();
    var existingJson = props.getProperty('DCA_RECORDS') || '[]';
    var list = JSON.parse(existingJson);
    var recId = p.recordId || ('REC-' + Date.now());
    var found = false;
    for (var i = 0; i < list.length; i++) {
      if (list[i].recordId === recId) {
        list[i] = p;
        found = true;
        break;
      }
    }
    if (!found) {
      list.unshift(p);
    }
    if (list.length > 200) list = list.slice(0, 200);
    props.setProperty('DCA_RECORDS', JSON.stringify(list));
  } catch(e) {}
}

function getMemoryRecords() {
  try {
    var props = PropertiesService.getScriptProperties();
    var existingJson = props.getProperty('DCA_RECORDS') || '[]';
    return JSON.parse(existingJson);
  } catch(e) {
    return [];
  }
}

function writeToDoc(docId,p){
  var doc=DocumentApp.openById(docId);
  var body=doc.getBody();
  var ts=Utilities.formatDate(new Date(),'Asia/Kolkata',"dd-MMM-yyyy HH:mm:ss 'IST'");
  body.appendParagraph('================================================').setHeading(DocumentApp.ParagraphHeading.NORMAL);
  body.appendParagraph('['+( p.instrumentType||'Survey')+']  '+(p.displayName||p.userId||'Unknown')+'  |  '+(p.city||'ND')+'  |  '+ts).setHeading(DocumentApp.ParagraphHeading.HEADING2);
  body.appendParagraph('Record:  '+(p.recordId||'-')+'\nUser:    '+(p.userId||'-')+'\nGPS:     '+(p.geoLat||'0')+', '+(p.geoLng||'0')+' (+/-'+Math.round(p.geoAccuracy||0)+'m)\nConsent: '+(p.consentGiven?'YES':'NO')+'\nStatus:  '+(p.status||'approved')).setHeading(DocumentApp.ParagraphHeading.NORMAL);
  var ans=p.answers;
  if(ans){
    body.appendParagraph('-- Responses --').setHeading(DocumentApp.ParagraphHeading.HEADING3);
    if(typeof ans==='object'&&!Array.isArray(ans)){
      var keys=Object.keys(ans);
      for(var i=0;i<keys.length;i++){var v=ans[keys[i]];if(v!==null&&v!==undefined&&String(v).trim()!==''){body.appendParagraph('Q: '+keys[i]+'\nA: '+String(v)).setHeading(DocumentApp.ParagraphHeading.NORMAL);}}
    }else if(typeof ans==='string'&&ans.trim().length>0){body.appendParagraph(ans);}
  }
  body.appendParagraph(' ');
}

function testWrite(){
  var p={token:'DCA_SECRET_TOKEN_2026',recordId:'TEST-'+Date.now(),userId:'SC0101',displayName:'Test User',city:'Nagpur',instrumentType:'Test Survey',geoLat:'21.14',geoLng:'79.08',geoAccuracy:5,consentGiven:true,status:'approved',answers:{'Question 1':'Answer A','Question 2':'Answer B'}};
  writeToDoc(USER_DOC_MAP[p.userId]||COMMON_DOC_ID,p);
  saveMemoryRecord(p);
  Logger.log('Done. Check SC0101 doc.');
}

function json(obj){return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);}

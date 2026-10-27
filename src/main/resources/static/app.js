/* ChildShield / CareTrack multi-page frontend.
   DB/API field contracts and proposed Spring Boot routes live in api-contract.js.
   This legacy app.js remains localStorage demo behavior until individual workflows are wired to API calls.
   Do not treat localStorage as database persistence or client-side role checks as authorization. */
const KEYS={users:"caretrack_users_v2",session:"caretrack_session_v2",beneficiaries:"caretrack_beneficiaries_v2",doses:"caretrack_doses_v2",notifications:"caretrack_notifications_v2",visits:"caretrack_visits_v2"};
const $=id=>document.getElementById(id);
const today=new Date();
const iso=(days=0)=>{const d=new Date();d.setDate(d.getDate()+days);return d.toISOString().slice(0,10)};
const fmtDate=v=>v?new Date(v+"T12:00:00").toLocaleDateString("en-IN",{day:"numeric",month:"short",year:"numeric"}):"—";
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const initials=s=>(s||"Demo User").trim().split(/\s+/).slice(0,2).map(x=>x[0]||"").join("").toUpperCase();
const id=prefix=>prefix+"-"+Math.random().toString(36).slice(2,8).toUpperCase();
function read(key,fallback){try{const x=localStorage.getItem(key);return x?JSON.parse(x):fallback}catch{return fallback}}
function write(key,value){localStorage.setItem(key,JSON.stringify(value))}
const seedBeneficiaries=[
{id:"CT-1001",name:"Aarav Patil",type:"Child",village:"Rampur",phone:"98XXXXXX12",registered:iso(-90),guardian:"Meera Patil"},
{id:"CT-1002",name:"Meera Patil",type:"Mother",village:"Rampur",phone:"98XXXXXX12",registered:iso(-90)},
{id:"CT-1003",name:"Ishita More",type:"Child",village:"Shivapur",phone:"97XXXXXX43",registered:iso(-70),guardian:"Kavita More"},
{id:"CT-1004",name:"Kavita More",type:"Mother",village:"Shivapur",phone:"97XXXXXX43",registered:iso(-70)},
{id:"CT-1005",name:"Vivaan Jadhav",type:"Child",village:"Devgaon",phone:"96XXXXXX27",registered:iso(-48),guardian:"Pooja Jadhav"},
{id:"CT-1006",name:"Pooja Jadhav",type:"Mother",village:"Devgaon",phone:"96XXXXXX27",registered:iso(-48)},
{id:"CT-1007",name:"Anaya Shinde",type:"Child",village:"Rampur",phone:"95XXXXXX61",registered:iso(-30),guardian:"Rani Shinde"},
{id:"CT-1008",name:"Rani Shinde",type:"Mother",village:"Rampur",phone:"95XXXXXX61",registered:iso(-30)}
];
const seedDoses=[
{id:"DS-001",beneficiaryId:"CT-1001",vaccine:"DPT booster",dose:"Demo dose 1",dueDate:iso(-8),status:"Overdue"},
{id:"DS-002",beneficiaryId:"CT-1003",vaccine:"Measles-Rubella",dose:"Demo dose 1",dueDate:iso(0),status:"Due"},
{id:"DS-003",beneficiaryId:"CT-1005",vaccine:"Polio",dose:"Demo dose 2",dueDate:iso(3),status:"Upcoming"},
{id:"DS-004",beneficiaryId:"CT-1007",vaccine:"Pentavalent",dose:"Demo dose 1",dueDate:iso(-3),status:"Overdue"},
{id:"DS-005",beneficiaryId:"CT-1001",vaccine:"Polio",dose:"Demo dose 1",dueDate:iso(-18),status:"Completed",administeredAt:iso(-17)},
{id:"DS-006",beneficiaryId:"CT-1003",vaccine:"BCG",dose:"Demo dose 1",dueDate:iso(-35),status:"Completed",administeredAt:iso(-35)},
{id:"DS-007",beneficiaryId:"CT-1005",vaccine:"Hepatitis B",dose:"Demo dose 1",dueDate:iso(7),status:"Upcoming"},
{id:"DS-008",beneficiaryId:"CT-1007",vaccine:"Polio",dose:"Demo dose 1",dueDate:iso(0),status:"Due"}
];
const seedNotifications=[
{id:id("NT"),beneficiaryId:"CT-1001",doseId:"DS-001",channel:"SMS",status:"Simulated",message:"A demo vaccination record is overdue.",createdAt:new Date(Date.now()-2700000).toISOString()},
{id:id("NT"),beneficiaryId:"CT-1003",doseId:"DS-002",channel:"In-app",status:"Queued",message:"A demo vaccination record is due today.",createdAt:new Date(Date.now()-10800000).toISOString()}
];
let beneficiaries=read(KEYS.beneficiaries,seedBeneficiaries),doses=read(KEYS.doses,seedDoses),notifications=read(KEYS.notifications,seedNotifications),visits=read(KEYS.visits,[]);
let users=read(KEYS.users,[]);
// Normalize older demo records to the guardian -> children one-to-many relationship.
beneficiaries.forEach(b=>{if(b.type==="Child"&&!b.guardianId&&b.guardian){const parent=beneficiaries.find(m=>m.type==="Mother"&&m.name===b.guardian);if(parent)b.guardianId=parent.id}});
const isMother=b=>b.type==="Mother";
const childrenOf=mother=>beneficiaries.filter(b=>b.type==="Child"&&(b.guardianId===mother.id||(!b.guardianId&&b.guardian===mother.name)));
const mothers=()=>beneficiaries.filter(isMother);
function persist(){write(KEYS.beneficiaries,beneficiaries);write(KEYS.doses,doses);write(KEYS.notifications,notifications);write(KEYS.visits,visits);write(KEYS.users,users)}
function currentUser(){return read(KEYS.session,null)}
function toast(message,error=false){const region=$("toastRegion");if(!region){alert(message);return}const el=document.createElement("div");el.className="toast"+(error?" error":"");el.textContent=message;region.appendChild(el);setTimeout(()=>el.remove(),3500)}
function person(b){return b?`<div class="flex items-center gap-3"><span class="grid h-9 w-9 flex-shrink-0 place-items-center rounded-xl ${b.type==="Mother"?"bg-orange-50 text-orange-700":"bg-care-50 text-care-700"} text-xs font-bold">${esc(initials(b.name))}</span><span><strong class="block text-sm text-slate-800">${esc(b.name)}</strong><small class="text-xs text-slate-400">${esc(b.id)}</small></span></div>`:"Unknown beneficiary"}
function beneficiary(id){return beneficiaries.find(b=>b.id===id)}
function status(d){if(d.status==="Completed")return "Completed";if(d.dueDate<iso())return "Overdue";if(d.dueDate===iso())return "Due";return "Upcoming"}
function pill(s){return `<span class="status status-${esc(s)}">${esc(s)}</span>`}
function goByRole(role){return role==="admin"?"admin-dashboard.html":role==="worker"?"health-worker-dashboard.html":"user-dashboard.html"}
function initAuth(){
 const page=location.pathname.split("/").pop()||"index.html";
 if(page==="index.html"){
  $("loginForm")?.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(e.currentTarget),email=String(f.get("email")).trim().toLowerCase(),password=String(f.get("password")),role=String(f.get("role"));if(!email||password.length<4){toast("Enter a valid email and a password of at least 4 characters.",true);return}const saved=users.find(u=>u.email===email);const account={name:saved?.name||email.split("@")[0],email,role:saved?.role||role,village:saved?.village||"Rampur"};write(KEYS.session,account);location.href=goByRole(account.role)});
 }
 if(page==="register.html"){
  $("registerForm")?.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(e.currentTarget),email=String(f.get("email")).trim().toLowerCase();if(users.some(u=>u.email===email)){toast("An account with that email already exists in this browser.",true);return}const u={name:String(f.get("name")).trim(),email,role:String(f.get("role")),village:String(f.get("village")||"").trim()||"Rampur"};users.push(u);persist();write(KEYS.session,{name:u.name,email:u.email,role:u.role,village:u.village});location.href=goByRole(u.role)});
 }
}
function initShell(){
 const user=currentUser();if(user){if($("sidebarIdentity"))$("sidebarIdentity").textContent=`${user.name} · ${user.role}`;if($("headerAvatar"))$("headerAvatar").textContent=initials(user.name)}
 $("logoutButton")?.addEventListener("click",()=>{localStorage.removeItem(KEYS.session);location.href="index.html"});
 $("mobileMenu")?.addEventListener("click",()=>{$("sidebar")?.classList.toggle("-translate-x-full")});
}
function updateDashboard(){
 const completed=doses.filter(d=>status(d)==="Completed").length,overdue=doses.filter(d=>status(d)==="Overdue").length,due=doses.filter(d=>["Due","Upcoming"].includes(status(d))&&d.dueDate<=iso(7)).length;
 [["totalBeneficiaries",beneficiaries.length],["completedCount",completed],["dueCount",due],["overdueCount",overdue],["adminBeneficiaries",beneficiaries.length],["adminDoses",doses.length],["adminOverdue",overdue],["adminNotifications",notifications.length],["reportBeneficiaries",beneficiaries.length],["reportCompleted",completed],["reportDue",doses.filter(d=>status(d)==="Due").length],["reportOverdue",overdue]].forEach(([k,v])=>{if($(k))$(k).textContent=v});
 const villageNames=[...new Set([...beneficiaries.map(b=>b.village),"Rampur","Shivapur","Devgaon"])];
 const html=villageNames.map(v=>{const members=beneficiaries.filter(b=>b.village===v).length,late=doses.filter(d=>beneficiary(d.beneficiaryId)?.village===v&&status(d)==="Overdue").length,done=doses.filter(d=>beneficiary(d.beneficiaryId)?.village===v&&status(d)==="Completed").length;return `<div class="grid grid-cols-[1fr_auto_auto] items-center gap-4 border-b border-slate-100 py-4 last:border-0"><div><strong class="text-sm">${esc(v)}</strong><p class="mt-1 text-xs text-slate-400">${members} registered beneficiaries</p><div class="mt-2 h-1.5 max-w-xs overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-care-500" style="width:${Math.min(100,members/Math.max(1,beneficiaries.length)*100)}%"></div></div></div><div class="text-right"><strong>${done}</strong><p class="text-xs text-slate-400">Completed</p></div><div class="text-right"><strong class="${late?'text-rose-600':'text-slate-600'}">${late}</strong><p class="text-xs text-slate-400">Overdue</p></div></div>`}).join("");
 ["villageOverview","adminVillageOverview"].forEach(k=>{if($(k))$(k).innerHTML=html});
 if($("attentionRows")){const list=doses.filter(d=>status(d)!=="Completed").sort((a,b)=>a.dueDate.localeCompare(b.dueDate)).slice(0,6);$("attentionRows").innerHTML=list.length?list.map(d=>{const b=beneficiary(d.beneficiaryId);return `<tr><td>${person(b)}</td><td>${esc(b?.village||"—")}</td><td>${esc(d.vaccine)}<br><small>${esc(d.dose)}</small></td><td>${fmtDate(d.dueDate)}</td><td>${pill(status(d))}</td><td><button data-complete="${d.id}" class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold hover:border-care-500">Record dose</button></td></tr>`}).join(""):`<tr><td colspan="6" class="text-center text-slate-400">No outstanding demo doses.</td></tr>`}
 const user=currentUser();if($("myChildrenCount")){$("myChildrenCount").textContent=beneficiaries.filter(b=>b.type==="Child").length;$("userCompletedCount").textContent=completed;$("userDueCount").textContent=due;$("userReminderCount").textContent=notifications.length;const list=doses.slice(0,5);$("userScheduleRows").innerHTML=list.map(d=>`<tr><td>${esc(d.vaccine)}<br><small>${esc(d.dose)}</small></td><td>${fmtDate(d.dueDate)}</td><td>${pill(status(d))}</td></tr>`).join("")}
}
function renderBeneficiaries(){
 if(!$ ("beneficiaryRows"))return;
 const q=($("beneficiarySearch")?.value||"").toLowerCase().trim(),v=$("villageFilter")?.value||"all";
 const list=mothers().filter(m=>{const kids=childrenOf(m);return(!q||[m.name,m.village,m.phone,m.id,...kids.map(c=>c.name)].some(x=>(x||"").toLowerCase().includes(q)))&&(v==="all"||m.village===v)}).sort((a,b)=>a.name.localeCompare(b.name));
 const rows=list.map(m=>{const kids=childrenOf(m);return `<tr><td>${person(m)}</td><td>${esc(m.village)}</td><td>${esc(m.phone||"—")}</td><td><span class="mr-2 inline-flex rounded-full bg-care-50 px-2.5 py-1 text-xs font-bold text-care-700">${kids.length}</span><button class="rounded-lg border border-care-200 px-3 py-2 text-xs font-bold text-care-700 hover:bg-care-50" data-view-children="${esc(m.id)}">View children</button></td><td>${fmtDate(m.registered)}</td></tr>`}).join("");
 $("beneficiaryRows").innerHTML=rows||`<tr><td colspan="5" class="py-8 text-center text-slate-400">No matching mothers/guardians found.</td></tr>`;
 $("beneficiaryResultCount").textContent=`Showing ${list.length} mothers/guardians · ${beneficiaries.filter(b=>b.type==="Child").length} children registered`;
}
function toggleChildren(motherId){
 const m=beneficiary(motherId);if(!m)return;const existing=document.querySelector(`[data-children-row="${CSS.escape(motherId)}"]`);if(existing){existing.remove();return}
 const anchor=document.querySelector(`[data-view-children="${CSS.escape(motherId)}"]`)?.closest("tr");if(!anchor)return;
 const kids=childrenOf(m);const row=document.createElement("tr");row.dataset.childrenRow=motherId;row.innerHTML=`<td colspan="6" class="!whitespace-normal !bg-care-50/60"><div class="rounded-xl border border-care-100 bg-white p-4"><div class="mb-3 flex items-center justify-between"><strong class="text-sm text-care-900">Children of ${esc(m.name)}</strong><span class="text-xs text-slate-500">${kids.length} child${kids.length===1?"":"ren"}</span></div>${kids.length?`<div class="grid gap-3 md:grid-cols-2">${kids.map(c=>`<div class="flex items-center justify-between gap-3 rounded-lg border border-slate-100 p-3"><div><strong class="block text-sm text-slate-800">${esc(c.name)}</strong><small class="text-xs text-slate-500">${esc(c.id)} · DOB: ${fmtDate(c.dateOfBirth||c.dob||"")}</small></div><span class="text-xs text-slate-500">${esc(c.gender||"Gender not specified")}</span></div>`).join("")}</div>`:`<p class="text-sm text-slate-500">No children registered yet.</p>`}</div></td>`;
 anchor.insertAdjacentElement("afterend",row);
}

function fillVillages(){["villageFilter","scheduleVillageFilter"].forEach(k=>{if(!$ (k))return;const old=$(k).value,vs=[...new Set(beneficiaries.map(b=>b.village))].sort();$(k).innerHTML='<option value="all">All villages</option>'+vs.map(v=>`<option value="${esc(v)}">${esc(v)}</option>`).join("");if(vs.includes(old))$(k).value=old})}
let doseFilter="all";
function renderSchedule(){
 if(!$("scheduleRows"))return;const q=($("scheduleSearch")?.value||"").toLowerCase().trim(),v=$("scheduleVillageFilter")?.value||"all";
 const list=doses.filter(d=>{const b=beneficiary(d.beneficiaryId);return(doseFilter==="all"||status(d)===doseFilter)&&(!q||[b?.name,b?.village,d.vaccine,d.dose].some(x=>(x||"").toLowerCase().includes(q)))&&(v==="all"||b?.village===v)}).sort((a,b)=>a.dueDate.localeCompare(b.dueDate));
 $("scheduleRows").innerHTML=list.length?list.map(d=>{const b=beneficiary(d.beneficiaryId),s=status(d);return `<tr><td>${person(b)}</td><td>${esc(b?.village||"—")}</td><td>${esc(d.vaccine)}<br><small>${esc(d.dose)}</small></td><td>${fmtDate(d.dueDate)}</td><td>${pill(s)}</td><td>${s==="Completed"?"Recorded":`<button data-complete="${d.id}" class="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold hover:border-care-500">Record dose</button>`}</td></tr>`}).join(""):`<tr><td colspan="5" class="py-8 text-center text-slate-400">No doses match these filters.</td></tr>`;
 $("scheduleResultCount").textContent=`Showing ${list.length} of ${doses.length} doses`;
}
function renderNotifications(){if(!$("notificationList"))return;const f=$("notificationFilter")?.value||"all",list=notifications.filter(n=>f==="all"||n.channel===f).sort((a,b)=>b.createdAt.localeCompare(a.createdAt));$("notificationList").innerHTML=list.length?list.map(n=>{const b=beneficiary(n.beneficiaryId),d=doses.find(x=>x.id===n.doseId);return `<article class="flex gap-4 border-b border-slate-100 py-5 last:border-0"><span class="grid h-10 w-10 flex-shrink-0 place-items-center rounded-xl ${n.channel==="SMS"?"bg-violet-50 text-violet-700":"bg-care-50 text-care-700"}">${n.channel==="SMS"?"✉":"♧"}</span><div class="min-w-0 flex-1"><strong class="text-sm">${esc(b?.name||"Beneficiary")} · ${esc(d?.vaccine||"Health reminder")}</strong><p class="mt-1 text-sm leading-6 text-slate-500">${esc(n.message)}</p><p class="mt-2 text-xs text-slate-400">${esc(n.channel==="SMS"?"Simulated SMS":"In-app")} · ${new Date(n.createdAt).toLocaleString("en-IN")} · ${esc(n.status)}</p></div></article>`}).join(""):`<div class="py-10 text-center text-sm text-slate-400">No notifications to show.</div>`}
function renderVisits(){if(!$("visitList"))return;$("visitList").innerHTML=visits.length?visits.slice().reverse().map(v=>{const b=beneficiary(v.beneficiaryId);return `<article class="rounded-xl border border-slate-200 p-4"><div class="flex items-start justify-between gap-3"><strong class="text-sm">${esc(v.visitType)}</strong><span class="text-xs text-slate-400">${fmtDate(v.visitDate)}</span></div><p class="mt-2 text-sm text-slate-600">${esc(b?.name||"Unknown beneficiary")}</p><p class="mt-1 text-xs text-slate-500">${esc(v.notes||"No notes added")}</p></article>`}).join(""):`<div class="rounded-xl bg-slate-50 p-6 text-center text-sm text-slate-400">No visit records yet. Add a fictional visit using the form.</div>`}
function renderReports(){if(!$("reportRows"))return;const vs=[...new Set(beneficiaries.map(b=>b.village))];$("reportRows").innerHTML=vs.map(v=>`<tr><td>${esc(v)}</td><td>${beneficiaries.filter(b=>b.village===v).length}</td><td>${doses.filter(d=>beneficiary(d.beneficiaryId)?.village===v&&status(d)==="Completed").length}</td><td>${doses.filter(d=>beneficiary(d.beneficiaryId)?.village===v&&status(d)==="Overdue").length}</td></tr>`).join("")}
function refresh(){persist();fillVillages();updateDashboard();renderBeneficiaries();renderSchedule();renderNotifications();renderVisits();renderReports();fillVisitBeneficiaries()}
function openModal(){if($("beneficiaryModal"))$("beneficiaryModal").hidden=false}
function closeModal(){if($("beneficiaryModal"))$("beneficiaryModal").hidden=true;$("beneficiaryForm")?.reset()}
function addBeneficiary(form){
 const f=new FormData(form),motherName=String(f.get("motherName")||"").trim(),phone=String(f.get("phone")||"").trim(),village=String(f.get("village")||"");
 const childNames=f.getAll("childName[]").map(x=>String(x||"").trim()),childDobs=f.getAll("childDob[]").map(String),childGenders=f.getAll("childGender[]").map(String),weights=f.getAll("birthWeight[]").map(String);
 if(!motherName||!phone||!village||!childNames.length||childNames.some((n,i)=>!n||!childDobs[i])){toast("Enter the mother details and at least one child's name and date of birth.",true);return}
 const mother={id:id("GR"),name:motherName,type:"Mother",village,phone,registered:iso()};beneficiaries.unshift(mother);
 childNames.forEach((name,i)=>{const child={id:id("CH"),name,type:"Child",village,phone,registered:iso(),guardian:mother.name,guardianId:mother.id,dateOfBirth:childDobs[i],gender:childGenders[i]||"",birthWeightKg:weights[i]?Number(weights[i]):null};beneficiaries.unshift(child);doses.unshift({id:id("DS"),beneficiaryId:child.id,vaccine:"Schedule to be assigned",dose:"Pending health-worker review",dueDate:iso(7),status:"Upcoming"})});
 refresh();closeModal();toast(`Mother registered with ${childNames.length} child${childNames.length===1?"":"ren"}.`)
}
function recordDose(doseId){const d=doses.find(x=>x.id===doseId);if(!d||status(d)==="Completed")return toast("This dose is already completed.");d.status="Completed";d.administeredAt=iso();refresh();toast("Demo dose marked as completed.");}
function simulateReminders(){const target=doses.filter(d=>["Due","Overdue"].includes(status(d)));if(!target.length)return toast("No due or overdue demo doses found.");target.forEach(d=>{const b=beneficiary(d.beneficiaryId);notifications.unshift({id:id("NT"),beneficiaryId:d.beneficiaryId,doseId:d.id,channel:b?.phone?"SMS":"In-app",status:b?.phone?"Simulated":"Queued",message:`Demo reminder: ${d.vaccine} is ${status(d).toLowerCase()}. Please contact your health worker.`,createdAt:new Date().toISOString()})});refresh();toast(`${target.length} reminder(s) simulated. No real SMS sent.`)}
function csvDownload(filename,rows){const csv=rows.map(r=>r.map(v=>`"${String(v??"").replace(/"/g,'""')}"`).join(",")).join("\\r\\n"),blob=new Blob([csv],{type:"text/csv;charset=utf-8"}),url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download=filename;a.click();URL.revokeObjectURL(url)}
function fillVisitBeneficiaries(){if(!$("visitBeneficiary"))return;const old=$("visitBeneficiary").value;$("visitBeneficiary").innerHTML=beneficiaries.map(b=>`<option value="${b.id}">${esc(b.name)} — ${esc(b.village)}</option>`).join("");if(beneficiaries.some(b=>b.id===old))$("visitBeneficiary").value=old}
function initPageEvents(){
 document.addEventListener("click",e=>{const complete=e.target.closest("[data-complete]");if(complete)recordDose(complete.dataset.complete);const children=e.target.closest("[data-view-children]");if(children){toggleChildren(children.dataset.viewChildren);return}const view=e.target.closest("[data-view]");if(view){const b=beneficiary(view.dataset.view);if(b)toast(`${b.name} · ${b.village} · ${b.phone||"No phone listed"}`)}});
 $("openBeneficiaryModal")?.addEventListener("click",openModal);$("closeBeneficiaryModal")?.addEventListener("click",closeModal);$("cancelBeneficiaryModal")?.addEventListener("click",closeModal);
 $("beneficiaryModal")?.addEventListener("click",e=>{if(e.target===$("beneficiaryModal"))closeModal()});
 $("beneficiaryForm")?.addEventListener("submit",e=>{e.preventDefault();addBeneficiary(e.currentTarget)});
 $("beneficiarySearch")?.addEventListener("input",renderBeneficiaries);$("villageFilter")?.addEventListener("change",renderBeneficiaries);
 $("addChildFormRow")?.addEventListener("click",()=>{const wrap=$("childFormRows");if(!wrap)return;const row=document.createElement("div");row.className="child-form-row grid gap-3 rounded-xl border border-slate-200 p-4 sm:grid-cols-2";row.innerHTML=`<div class="sm:col-span-2 flex justify-end"><button type="button" class="remove-child-row text-xs font-bold text-rose-600 hover:underline">Remove child</button></div><label class="text-sm font-semibold">Child full name *<input name="childName[]" required maxlength="120" placeholder="Child full name" class="mt-2 h-11 w-full rounded-xl border border-slate-200 px-3 text-sm font-normal"></label><label class="text-sm font-semibold">Date of birth *<input name="childDob[]" required type="date" class="mt-2 h-11 w-full rounded-xl border border-slate-200 px-3 text-sm font-normal"></label><label class="text-sm font-semibold">Gender<select name="childGender[]" class="mt-2 h-11 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm font-normal"><option value="">Prefer not to say</option><option>Female</option><option>Male</option><option>Other</option></select></label><label class="text-sm font-semibold">Birth weight (kg, optional)<input name="birthWeight[]" type="number" min="0.5" max="10" step="0.01" placeholder="e.g. 3.20" class="mt-2 h-11 w-full rounded-xl border border-slate-200 px-3 text-sm font-normal"></label>`;wrap.appendChild(row)});
 $("childFormRows")?.addEventListener("click",e=>{const btn=e.target.closest(".remove-child-row");if(!btn)return;const rows=$("childFormRows").querySelectorAll(".child-form-row");if(rows.length<=1){toast("At least one child is required.",true);return}btn.closest(".child-form-row").remove()});
 ["scheduleSearch","scheduleVillageFilter"].forEach(k=>$(k)?.addEventListener(k==="scheduleSearch"?"input":"change",renderSchedule));
 $("scheduleTabs")?.addEventListener("click",e=>{const b=e.target.closest("[data-status]");if(!b)return;doseFilter=b.dataset.status;$("scheduleTabs").querySelectorAll("button").forEach(x=>{x.className=x===b?"rounded-lg bg-care-100 px-3 py-2 text-xs font-bold text-care-700":"rounded-lg bg-white px-3 py-2 text-xs text-slate-600"});renderSchedule()});
 $("notificationFilter")?.addEventListener("change",renderNotifications);$("sendReminders")?.addEventListener("click",simulateReminders);
 $("exportSchedule")?.addEventListener("click",()=>csvDownload("caretrack-demo-schedule.csv",[["Beneficiary","Village","Vaccine","Dose","Due date","Status"],...doses.map(d=>{const b=beneficiary(d.beneficiaryId);return[b?.name,b?.village,d.vaccine,d.dose,d.dueDate,status(d)]})]));
 $("exportReport")?.addEventListener("click",()=>csvDownload("caretrack-demo-village-report.csv",[["Village","Beneficiaries","Completed doses","Overdue doses"],...[...new Set(beneficiaries.map(b=>b.village))].map(v=>[v,beneficiaries.filter(b=>b.village===v).length,doses.filter(d=>beneficiary(d.beneficiaryId)?.village===v&&status(d)==="Completed").length,doses.filter(d=>beneficiary(d.beneficiaryId)?.village===v&&status(d)==="Overdue").length])]));
 $("visitForm")?.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(e.currentTarget);visits.push({id:id("VS"),beneficiaryId:String(f.get("beneficiaryId")),visitType:String(f.get("visitType")),visitDate:String(f.get("visitDate")),notes:String(f.get("notes")||"").trim()});e.currentTarget.reset();refresh();toast("Demo visit record saved.")});
 $("profileForm")?.addEventListener("submit",e=>{e.preventDefault();const f=new FormData(e.currentTarget),u=currentUser()||{};const updated={...u,name:String(f.get("name")).trim(),email:String(f.get("email")).trim().toLowerCase(),village:String(f.get("village")||"").trim()};write(KEYS.session,updated);users=users.map(x=>x.email===u.email?{...x,...updated}:x);persist();renderProfile();initShell();toast("Demo profile updated.")});
}
function renderProfile(){const u=currentUser()||{name:"Demo User",email:"demo@example.com",role:"worker",village:"Rampur"};if($("profileName"))$("profileName").textContent=u.name;if($("profileRole"))$("profileRole").textContent=({user:"Mother / Guardian",worker:"Health worker",admin:"Administrator"})[u.role]||u.role;if($("profileAvatar"))$("profileAvatar").textContent=initials(u.name);const f=$("profileForm");if(f){f.elements.name.value=u.name||"";f.elements.email.value=u.email||"";f.elements.village.value=u.village||""}}
function bootstrap(){initAuth();initShell();initPageEvents();renderProfile();refresh()}
bootstrap();

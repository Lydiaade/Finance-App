function convertDateTime(date) {
  return `${new Date(date).toLocaleDateString()} ${new Date(
    date
  ).toLocaleTimeString()}`;
}

// Returns today's date as a yyyy-MM-dd string, based on local time rather
// than UTC, so it lines up with what a native <input type="date"> produces
// and avoids the day-shift bug you get from new Date().toISOString().
function getTodayIsoDate() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// Formats an ISO (yyyy-MM-dd) date string as dd/MM/yyyy for display,
// without constructing a Date object — avoids timezone-related day-shift
// bugs when parsing a plain date (no time component) string.
function formatIsoDateForDisplay(isoDate) {
  if (!isoDate) return "";
  const [year, month, day] = isoDate.split("-");
  if (!year || !month || !day) return isoDate;
  return `${day}/${month}/${year}`;
}

export { convertDateTime, getTodayIsoDate, formatIsoDateForDisplay };

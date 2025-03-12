function convertDateTime(date) {
  return `${new Date(date).toLocaleDateString()} ${new Date(
    date
  ).toLocaleTimeString()}`;
}

export { convertDateTime };

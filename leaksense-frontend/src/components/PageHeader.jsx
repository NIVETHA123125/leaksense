function PageHeader({ eyebrow, title, subtitle }) {
  return (
    <div className="page-header">
      <div className="page-eyebrow">{eyebrow}</div>
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </div>
  )
}

export default PageHeader
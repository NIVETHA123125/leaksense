import PageHeader from '../components/PageHeader'
import ReportButtons from '../components/ReportButtons'
import AiReportSummaryCard from '../components/AiReportSummaryCard'

export default function ReportsPage() {
  return (
    <>
      <PageHeader
        eyebrow="SAFETY DOCUMENTATION & REPORTS"
        title="Compliance & Safety Audits"
        subtitle="Automated executive PDF dispatch, historical Excel exports, and AI risk evaluations"
      />
      <AiReportSummaryCard />
      <ReportButtons />
    </>
  )
}
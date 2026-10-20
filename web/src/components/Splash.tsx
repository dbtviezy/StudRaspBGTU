export default function Splash() {
  return (
    <div className="splash">
      <div className="seal">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="/icons/mark.png" alt="БГТУ" />
      </div>
      <div className="foot">
        <div style={{ lineHeight: "18px" }}>Брянский государственный технический университет</div>
        <div className="semibold mt6" style={{ fontSize: 16 }}>
          СтудРаспБГТУ
        </div>
      </div>
    </div>
  );
}

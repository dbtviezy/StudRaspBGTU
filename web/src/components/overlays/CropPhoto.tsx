"use client";

import { useEffect, useRef, useState } from "react";
import type { Copy } from "@/lib/i18n";
import { Dialog } from "../ui";

interface Props {
  file: File;
  s: Copy;
  onCancel: () => void;
  onSave: (blob: Blob) => void;
}

export default function CropPhoto({ file, s, onCancel, onSave }: Props) {
  const box = useRef<HTMLDivElement>(null);
  const [url] = useState(() => URL.createObjectURL(file));
  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [boxSize, setBoxSize] = useState(300);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const pointers = useRef(new Map<number, { x: number; y: number }>());
  const pinch = useRef<{ distance: number; zoom: number } | null>(null);

  useEffect(() => () => URL.revokeObjectURL(url), [url]);
  useEffect(() => {
    const el = box.current;
    if (!el) return;
    const observer = new ResizeObserver(() => setBoxSize(el.clientWidth));
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const base = image ? Math.max(boxSize / image.naturalWidth, boxSize / image.naturalHeight) : 1;
  const scale = base * zoom;
  const width = image ? image.naturalWidth * scale : boxSize;
  const height = image ? image.naturalHeight * scale : boxSize;

  const clamp = (x: number, y: number, w = width, h = height) => ({
    x: Math.max(-(w - boxSize) / 2, Math.min((w - boxSize) / 2, x)),
    y: Math.max(-(h - boxSize) / 2, Math.min((h - boxSize) / 2, y)),
  });

  const setZoomClamped = (value: number) => {
    const next = Math.max(1, Math.min(4, value));
    setZoom(next);
    if (image) setOffset((o) => clamp(o.x, o.y, image.naturalWidth * base * next, image.naturalHeight * base * next));
  };

  const onPointerDown = (event: React.PointerEvent) => {
    (event.target as HTMLElement).setPointerCapture?.(event.pointerId);
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
    if (pointers.current.size === 2) {
      const [a, b] = [...pointers.current.values()];
      pinch.current = { distance: Math.hypot(a.x - b.x, a.y - b.y), zoom };
    }
  };

  const onPointerMove = (event: React.PointerEvent) => {
    const previous = pointers.current.get(event.pointerId);
    if (!previous) return;
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY });
    if (pointers.current.size === 2 && pinch.current) {
      const [a, b] = [...pointers.current.values()];
      setZoomClamped((pinch.current.zoom * Math.hypot(a.x - b.x, a.y - b.y)) / pinch.current.distance);
      return;
    }
    setOffset((o) => clamp(o.x + event.clientX - previous.x, o.y + event.clientY - previous.y));
  };

  const onPointerUp = (event: React.PointerEvent) => {
    pointers.current.delete(event.pointerId);
    if (pointers.current.size < 2) pinch.current = null;
  };

  const save = () => {
    if (!image) return;
    const canvas = document.createElement("canvas");
    canvas.width = 512;
    canvas.height = 512;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    const size = boxSize / scale;
    const sx = image.naturalWidth / 2 - offset.x / scale - size / 2;
    const sy = image.naturalHeight / 2 - offset.y / scale - size / 2;
    ctx.drawImage(image, sx, sy, size, size, 0, 0, 512, 512);
    canvas.toBlob((blob) => blob && onSave(blob), "image/jpeg", 0.92);
  };

  return (
    <Dialog onClose={onCancel}>
      <h2 className="h2">{s.cropTitle}</h2>
      <div className="muted small mt4">{s.cropHint}</div>
      <div
        ref={box}
        className="crop-box mt12"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onWheel={(e) => setZoomClamped(zoom - e.deltaY / 500)}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={url}
          alt=""
          onLoad={(e) => setImage(e.currentTarget)}
          style={{ width, height, transform: `translate(calc(-50% + ${offset.x}px), calc(-50% + ${offset.y}px))` }}
        />
        <div className="ring" />
      </div>
      <input className="mt12" type="range" min={1} max={4} step={0.01} value={zoom} onChange={(e) => setZoomClamped(Number(e.target.value))} aria-label="zoom" />
      <div className="dialog-actions">
        <button className="tbtn muted" onClick={onCancel}>{s.cancel}</button>
        <button className="tbtn" onClick={save} disabled={!image}>{s.save}</button>
      </div>
    </Dialog>
  );
}

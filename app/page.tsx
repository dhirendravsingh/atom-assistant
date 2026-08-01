"use client";

import {
  AlarmClock,
  ArrowLeft,
  Bell,
  CalendarDays,
  Check,
  ChevronRight,
  CircleUserRound,
  Clock3,
  Cloud,
  Edit3,
  Home,
  ListTodo,
  Mic,
  Moon,
  Plus,
  RotateCcw,
  Search,
  Settings,
  Sparkles,
  Sun,
  Trash2,
  Volume2,
  X,
  Zap,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

type Screen = "home" | "capture" | "reminders" | "settings";
type Meridiem = "AM" | "PM";
type Theme = "light" | "dark";
type LogoVariant =
  | "original"
  | "orbit"
  | "pulse"
  | "spark"
  | "nucleus"
  | "halo"
  | "bond"
  | "mono"
  | "prism"
  | "twin"
  | "eclipse"
  | "ripple"
  | "node"
  | "arc";

const OWNER_NAME = "Dhiren Sir";

const LOGO_OPTIONS: { id: LogoVariant; label: string }[] = [
  { id: "original", label: "Original" },
  { id: "orbit", label: "Orbit" },
  { id: "pulse", label: "Pulse" },
  { id: "spark", label: "Spark" },
  { id: "nucleus", label: "Nucleus" },
  { id: "halo", label: "Halo" },
  { id: "bond", label: "Bond" },
  { id: "mono", label: "Mono A" },
  { id: "prism", label: "Prism" },
  { id: "twin", label: "Twin" },
  { id: "eclipse", label: "Eclipse" },
  { id: "ripple", label: "Ripple" },
  { id: "node", label: "Node" },
  { id: "arc", label: "Arc" },
];

type Reminder = {
  id: number;
  title: string;
  dateLabel: string | null;
  time: string | null;
  meridiem: Meridiem | null;
  rawText: string;
  source: "Voice" | "Text";
  state: "scheduled" | "needs_time" | "needs_date" | "unscheduled";
  accent: "mint" | "coral" | "lime";
  recurrenceRule: string | null;
  recurrenceLabel: string | null;
};

type Draft = {
  title: string;
  dateLabel: string | null;
  time: string | null;
  meridiem: Meridiem | null;
  rawText: string;
  source: "Voice" | "Text";
  relativeLabel?: string;
  recurrenceRule: string | null;
  recurrenceLabel: string | null;
};

const PREFIXES = [
  "Atom",
  "Hey Atom",
  "Hi Atom",
  "Hello Atom",
  "Okay Atom",
  "OK Atom",
  "Atom, please",
  "Please remind me",
  "Can you remind me",
  "Could you remind me",
  "Would you remind me",
  "Remind me",
  "Remind me again",
  "I want you to remind me",
  "I would like a reminder",
  "I need a reminder",
  "Set a reminder",
  "Give me a reminder",
  "Create a reminder",
  "Schedule a reminder",
  "Help me remember",
  "Don't let me forget",
  "Don't forget to remind me",
  "Make sure I remember",
  "Make sure I don't forget",
  "Ping me",
  "Alert me",
  "Notify me",
  "Nudge me",
  "Remember that I need to",
  "When it's time",
  "At that time remind me",
  "Do me a favor and remind me",
  "Could you please remind me",
  "Would you please remind me",
  "I must remember to",
  "I have to remember to",
  "Add this to my reminders",
  "Put this on my reminder list",
  "Make a note to remind me",
  "Keep this on my radar",
  "Give me a heads-up",
  "Let me know when it's time to",
  "Tell me when it's time to",
  "Wake me up to",
  "Set an alert for",
  "Schedule this for",
];

const INITIAL_REMINDERS: Reminder[] = [
  {
    id: 1,
    title: "Send product brief to Aisha",
    dateLabel: "Today · Jul 28",
    time: "6:30",
    meridiem: "PM",
    rawText:
      "Hey Atom, remind me to send the product brief to Aisha today at 6:30 PM.",
    source: "Voice",
    state: "scheduled",
    accent: "mint",
    recurrenceRule: null,
    recurrenceLabel: null,
  },
  {
    id: 2,
    title: "Renew car insurance",
    dateLabel: "Tomorrow · Jul 29",
    time: "10:00",
    meridiem: "AM",
    rawText: "Please remind me to renew my car insurance tomorrow at 10 AM.",
    source: "Text",
    state: "scheduled",
    accent: "coral",
    recurrenceRule: null,
    recurrenceLabel: null,
  },
  {
    id: 3,
    title: "Call Rhea about the launch",
    dateLabel: "Friday · Jul 31",
    time: null,
    meridiem: null,
    rawText: "Atom, remind me to call Rhea about the launch on Friday.",
    source: "Voice",
    state: "needs_time",
    accent: "lime",
    recurrenceRule: null,
    recurrenceLabel: null,
  },
  {
    id: 4,
    title: "Review today’s priorities",
    dateLabel: "Every weekday",
    time: "9:00",
    meridiem: "AM",
    rawText:
      "Every weekday at 9 AM remind me to review today’s priorities.",
    source: "Voice",
    state: "scheduled",
    accent: "mint",
    recurrenceRule: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
    recurrenceLabel: "Every weekday",
  },
];

const SAMPLE_COMMANDS = [
  "Remind me in 20 minutes to check the oven",
  "Please remind me to call Rhea",
  "Remind me to prepare the deck tomorrow",
  "At 4:30 PM remind me to call home",
  "Every weekday at 9 AM remind me to review my priorities",
];

const RECURRENCE_OPTIONS = [
  { label: "Doesn’t repeat", rule: "" },
  { label: "Every day", rule: "FREQ=DAILY" },
  {
    label: "Every weekday",
    rule: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
  },
  { label: "Every week", rule: "FREQ=WEEKLY" },
  { label: "Every month", rule: "FREQ=MONTHLY" },
];

const WEEKDAY_CODES: Record<string, string> = {
  monday: "MO",
  tuesday: "TU",
  wednesday: "WE",
  thursday: "TH",
  friday: "FR",
  saturday: "SA",
  sunday: "SU",
};

function formatClock(date: Date) {
  let hours = date.getHours();
  const meridiem: Meridiem = hours >= 12 ? "PM" : "AM";
  hours = hours % 12 || 12;
  return {
    time: `${hours}:${String(date.getMinutes()).padStart(2, "0")}`,
    meridiem,
  };
}

function cleanTask(rawText: string) {
  let value = rawText.trim();
  const prefixPattern = new RegExp(
    `^(?:${[...PREFIXES]
      .sort((a, b) => b.length - a.length)
      .map((prefix) => prefix.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"))
      .join("|")})[\\s,:-]*`,
    "i",
  );

  for (let index = 0; index < 3; index += 1) {
    value = value.replace(prefixPattern, "");
  }

  value = value
    .replace(/^(?:to|about|that)\s+/i, "")
    .replace(
      /^(?:remind me(?: again)?(?: to| about)?|set (?:me )?a reminder(?: to| for)?|create a reminder(?: to| for)?|schedule a reminder(?: to| for)?|don't let me forget to|make sure i remember to|ping me to|alert me to|notify me to|nudge me to)\s*/i,
      "",
    )
    .replace(/\bin\s+\d+\s+(?:minutes?|hours?|days?)\b/gi, "")
    .replace(
      /\b(?:every\s+(?:day|weekday|week|month|monday|tuesday|wednesday|thursday|friday|saturday|sunday)|daily|weekdays|weekly|monthly)\b/gi,
      "",
    )
    .replace(/\b(?:today|tomorrow|day after tomorrow)\b/gi, "")
    .replace(
      /\b(?:on\s+)?(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b/gi,
      "",
    )
    .replace(
      /\b(?:at\s+)?(?:\d{1,2})(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)\b/gi,
      "",
    )
    .replace(/\b(?:at\s+)?(?:noon|midnight)\b/gi, "")
    .replace(/\s+/g, " ")
    .replace(/^[,.\s-]+|[,.\s-]+$/g, "")
    .trim();

  if (!value) return "Untitled reminder";
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function parseCommand(rawText: string, source: "Voice" | "Text"): Draft {
  const normalized = rawText.toLowerCase();
  const now = new Date();
  let dateLabel: string | null = null;
  let time: string | null = null;
  let meridiem: Meridiem | null = null;
  let relativeLabel: string | undefined;
  let recurrenceRule: string | null = null;
  let recurrenceLabel: string | null = null;

  if (/\b(?:every day|daily)\b/.test(normalized)) {
    recurrenceRule = "FREQ=DAILY";
    recurrenceLabel = "Every day";
  } else if (/\b(?:every weekday|weekdays)\b/.test(normalized)) {
    recurrenceRule = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR";
    recurrenceLabel = "Every weekday";
  } else {
    const recurringWeekday = normalized.match(
      /\bevery\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b/,
    );
    if (recurringWeekday) {
      const day = recurringWeekday[1];
      recurrenceRule = `FREQ=WEEKLY;BYDAY=${WEEKDAY_CODES[day]}`;
      recurrenceLabel = `Every ${day.charAt(0).toUpperCase()}${day.slice(1)}`;
    } else if (/\b(?:every week|weekly)\b/.test(normalized)) {
      recurrenceRule = "FREQ=WEEKLY";
      recurrenceLabel = "Every week";
    } else if (/\b(?:every month|monthly)\b/.test(normalized)) {
      recurrenceRule = "FREQ=MONTHLY";
      recurrenceLabel = "Every month";
    }
  }

  const relativeMatch = normalized.match(
    /\bin\s+(\d+)\s+(minute|minutes|hour|hours|day|days)\b/,
  );

  if (relativeMatch) {
    const amount = Number(relativeMatch[1]);
    const unit = relativeMatch[2];
    const multiplier = unit.startsWith("minute")
      ? 60_000
      : unit.startsWith("hour")
        ? 3_600_000
        : 86_400_000;
    const target = new Date(now.getTime() + amount * multiplier);
    const formatted = formatClock(target);
    time = formatted.time;
    meridiem = formatted.meridiem;
    dateLabel = unit.startsWith("day") ? `In ${amount} days` : "Today";
    relativeLabel = `In ${amount} ${unit}`;
  } else {
    if (recurrenceLabel) {
      dateLabel = recurrenceLabel;
    } else if (normalized.includes("day after tomorrow")) {
      dateLabel = "Thursday · Jul 30";
    } else if (normalized.includes("tomorrow")) {
      dateLabel = "Tomorrow · Jul 29";
    } else if (normalized.includes("today")) {
      dateLabel = "Today · Jul 28";
    } else {
      const weekdays: Record<string, string> = {
        monday: "Monday · Aug 3",
        tuesday: "Tuesday · Aug 4",
        wednesday: "Wednesday · Jul 29",
        thursday: "Thursday · Jul 30",
        friday: "Friday · Jul 31",
        saturday: "Saturday · Aug 1",
        sunday: "Sunday · Aug 2",
      };
      const weekday = Object.keys(weekdays).find((day) =>
        normalized.includes(day),
      );
      if (weekday) dateLabel = weekdays[weekday];
    }

    if (normalized.includes("midnight")) {
      time = "12:00";
      meridiem = "AM";
    } else if (normalized.includes("noon")) {
      time = "12:00";
      meridiem = "PM";
    } else {
      const timeMatch = normalized.match(
        /\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)\b/,
      );
      if (timeMatch) {
        const hour = Math.max(1, Math.min(12, Number(timeMatch[1])));
        time = `${hour}:${timeMatch[2] ?? "00"}`;
        meridiem = timeMatch[3].startsWith("a") ? "AM" : "PM";
      }
    }
  }

  return {
    title: cleanTask(rawText),
    dateLabel,
    time,
    meridiem,
    rawText,
    source,
    relativeLabel,
    recurrenceRule,
    recurrenceLabel,
  };
}

function stateForDraft(draft: Draft): Reminder["state"] {
  if (draft.dateLabel && draft.time) return "scheduled";
  if (draft.dateLabel) return "needs_time";
  if (draft.time) return "needs_date";
  return "unscheduled";
}

function StatusBar() {
  return (
    <div className="status-bar" aria-hidden="true">
      <span>9:41</span>
      <div className="status-icons">
        <span className="signal">▮▮▮</span>
        <span className="wifi">◒</span>
        <span className="battery">
          <span />
        </span>
      </div>
    </div>
  );
}

function AtomMark({
  compact = false,
  variant = "original",
  wordmark = true,
}: {
  compact?: boolean;
  variant?: LogoVariant;
  wordmark?: boolean;
}) {
  return (
    <div
      className={`atom-mark mark-${variant} ${compact ? "compact" : ""} ${wordmark ? "" : "glyph-only"}`}
    >
      <span className="atom-glyph" aria-hidden="true">
        <i />
        <b />
      </span>
      {wordmark && <span>atom</span>}
    </div>
  );
}

function AtomDoodle() {
  return (
    <div className="atom-doodle" role="img" aria-label="Atom waves hello">
      <span className="doodle-orbit orbit-a" />
      <span className="doodle-orbit orbit-b" />
      <span className="doodle-head">
        <i />
        <b />
      </span>
      <span className="doodle-body" />
      <span className="doodle-arm arm-left" />
      <span className="doodle-arm arm-wave">
        <i />
      </span>
      <small>hi</small>
    </div>
  );
}

function ReminderCard({
  reminder,
  onClick,
  compact = false,
}: {
  reminder: Reminder;
  onClick: () => void;
  compact?: boolean;
}) {
  return (
    <button
      className={`reminder-card accent-${reminder.accent} ${compact ? "compact" : ""}`}
      onClick={onClick}
      type="button"
      data-testid={`reminder-${reminder.id}`}
    >
      <span className="reminder-accent" />
      <span className="reminder-card-main">
        <span className="reminder-kicker">
          {reminder.state === "scheduled" ? (
            <>
              {reminder.recurrenceLabel ? (
                <RotateCcw size={13} />
              ) : (
                <Clock3 size={13} />
              )}
              {reminder.dateLabel}
            </>
          ) : (
            <>
              <Sparkles size={13} />
              Needs {reminder.state === "needs_time" ? "a time" : "details"}
            </>
          )}
        </span>
        <strong>{reminder.title}</strong>
        <small>
          {reminder.time ? (
            <>
              {reminder.time} <em>{reminder.meridiem}</em>
              {reminder.recurrenceLabel &&
                reminder.recurrenceLabel !== reminder.dateLabel && (
                <span className="recurrence-inline">
                  <RotateCcw size={10} />
                  {reminder.recurrenceLabel}
                </span>
                )}
            </>
          ) : (
            "Saved safely · not scheduled yet"
          )}
        </small>
      </span>
      <span className="reminder-card-action">
        <ChevronRight size={18} />
      </span>
    </button>
  );
}

function BottomNav({
  screen,
  onChange,
}: {
  screen: Screen;
  onChange: (screen: Screen) => void;
}) {
  const item = (
    target: Screen,
    label: string,
    icon: React.ReactNode,
    primary = false,
  ) => (
    <button
      className={`${screen === target ? "active" : ""} ${primary ? "primary" : ""}`}
      onClick={() => onChange(target)}
      type="button"
      aria-label={label}
      data-testid={`nav-${target}`}
    >
      <span>{icon}</span>
      {!primary && <small>{label}</small>}
    </button>
  );

  return (
    <nav className="bottom-nav" aria-label="Primary navigation">
      {item("home", "Today", <Home size={20} />)}
      {item("reminders", "Reminders", <ListTodo size={20} />)}
      {item("capture", "Add reminder", <Plus size={24} />, true)}
      {item("settings", "Settings", <Settings size={20} />)}
    </nav>
  );
}

export default function HomePage() {
  const [screen, setScreen] = useState<Screen>("home");
  const [reminders, setReminders] = useState(INITIAL_REMINDERS);
  const [captureText, setCaptureText] = useState("");
  const [captureSource, setCaptureSource] = useState<"Voice" | "Text">("Text");
  const [isListening, setIsListening] = useState(false);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [followUp, setFollowUp] = useState(false);
  const [followListening, setFollowListening] = useState(false);
  const [followText, setFollowText] = useState("");
  const [editing, setEditing] = useState<Reminder | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDate, setEditDate] = useState("");
  const [editTime, setEditTime] = useState("");
  const [editMeridiem, setEditMeridiem] = useState<Meridiem>("AM");
  const [editRaw, setEditRaw] = useState("");
  const [editCommand, setEditCommand] = useState("");
  const [editNote, setEditNote] = useState("");
  const [editRecurrenceRule, setEditRecurrenceRule] = useState("");
  const [editRecurrenceLabel, setEditRecurrenceLabel] = useState("");
  const [cancelConfirm, setCancelConfirm] = useState(false);
  const [alarmOpen, setAlarmOpen] = useState(false);
  const [snoozeOpen, setSnoozeOpen] = useState(false);
  const [cloudFallback, setCloudFallback] = useState(false);
  const [toast, setToast] = useState("");
  const [activeFilter, setActiveFilter] = useState("Upcoming");
  const [theme, setTheme] = useState<Theme>("light");
  const [logoVariant, setLogoVariant] = useState<LogoVariant>("original");
  const [logoSheetOpen, setLogoSheetOpen] = useState(false);
  const [currentTime, setCurrentTime] = useState<Date | null>(null);
  const [detectedTimezone, setDetectedTimezone] = useState("Asia/Kolkata");
  const [detectedLocale, setDetectedLocale] = useState("English (India)");

  useEffect(() => {
    const preferredTheme = window.localStorage.getItem("atom-theme");
    if (preferredTheme === "dark" || preferredTheme === "light") {
      setTheme(preferredTheme);
    } else if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
      setTheme("dark");
    }

    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (zone) setDetectedTimezone(zone);
    if (navigator.language) {
      const displayName = new Intl.DisplayNames(["en"], { type: "language" });
      const languageName = displayName.of(navigator.language.split("-")[0]);
      setDetectedLocale(
        `${languageName ?? "English"}${navigator.language.includes("-") ? ` (${navigator.language.split("-")[1].toUpperCase()})` : ""}`,
      );
    }

    setCurrentTime(new Date());
    const timer = window.setInterval(() => setCurrentTime(new Date()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(""), 2800);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const nextReminder = useMemo(
    () => reminders.find((item) => item.state === "scheduled") ?? reminders[0],
    [reminders],
  );

  const greeting =
    !currentTime
      ? "Hello"
      : currentTime.getHours() < 5
      ? "Good night"
      : currentTime.getHours() < 12
        ? "Good morning"
        : currentTime.getHours() < 17
          ? "Good afternoon"
          : currentTime.getHours() < 22
            ? "Good evening"
            : "Good night";

  const todayLabel = currentTime
    ? new Intl.DateTimeFormat("en-IN", {
        weekday: "long",
        day: "numeric",
        month: "long",
      }).format(currentTime)
    : "Today";

  function toggleTheme() {
    setTheme((current) => {
      const next = current === "light" ? "dark" : "light";
      window.localStorage.setItem("atom-theme", next);
      return next;
    });
  }

  function navigate(next: Screen) {
    setScreen(next);
    setDraft(null);
    setFollowUp(false);
    if (next === "capture") {
      setCaptureText("");
      setCaptureSource("Text");
    }
  }

  function simulateVoice(text = "Hey Atom, remind me to send the proposal tomorrow at 12 PM") {
    setScreen("capture");
    setCaptureSource("Voice");
    setCaptureText("");
    setDraft(null);
    setFollowUp(false);
    setIsListening(true);
    window.setTimeout(() => {
      setCaptureText(text);
      setIsListening(false);
    }, 850);
  }

  function understand(text = captureText, source = captureSource) {
    if (!text.trim()) {
      setToast("Tell Atom what you want to remember.");
      return;
    }
    const parsed = parseCommand(text, source);
    setDraft(parsed);
    if (!parsed.dateLabel || !parsed.time) {
      setFollowUp(true);
      setFollowText("");
    }
  }

  function applyFollowUp() {
    if (!draft) return;
    let next = { ...draft };
    if (followText.trim()) {
      const extra = parseCommand(followText, "Voice");
      next = {
        ...next,
        dateLabel: extra.dateLabel ?? next.dateLabel,
        time: extra.time ?? next.time,
        meridiem: extra.meridiem ?? next.meridiem,
        recurrenceRule: extra.recurrenceRule ?? next.recurrenceRule,
        recurrenceLabel: extra.recurrenceLabel ?? next.recurrenceLabel,
      };
    }
    if (!next.dateLabel) next.dateLabel = "Tomorrow · Jul 29";
    if (!next.time) {
      next.time = "6:30";
      next.meridiem = "PM";
    }
    setDraft(next);
    setFollowUp(false);
    setFollowListening(false);
  }

  function saveDraft() {
    if (!draft) return;
    const state = stateForDraft(draft);
    const reminder: Reminder = {
      id: Date.now(),
      title: draft.title,
      dateLabel: draft.dateLabel,
      time: draft.time,
      meridiem: draft.meridiem,
      rawText: draft.rawText,
      source: draft.source,
      state,
      accent: reminders.length % 2 === 0 ? "mint" : "coral",
      recurrenceRule: draft.recurrenceRule,
      recurrenceLabel: draft.recurrenceLabel,
    };
    setReminders((items) => [reminder, ...items]);
    setDraft(null);
    setCaptureText("");
    setScreen("home");
    setToast(
      state === "scheduled"
        ? "Reminder scheduled. Atom is on it."
        : "Saved to Unscheduled.",
    );
  }

  function saveWithoutSchedule() {
    if (!draft) return;
    const reminder: Reminder = {
      id: Date.now(),
      title: draft.title,
      dateLabel: draft.dateLabel,
      time: draft.time,
      meridiem: draft.meridiem,
      rawText: draft.rawText,
      source: draft.source,
      state: stateForDraft(draft),
      accent: "lime",
      recurrenceRule: draft.recurrenceRule,
      recurrenceLabel: draft.recurrenceLabel,
    };
    setReminders((items) => [reminder, ...items]);
    setDraft(null);
    setFollowUp(false);
    setScreen("reminders");
    setActiveFilter("Unscheduled");
    setToast("Saved. Atom will keep it in Unscheduled.");
  }

  function openEditor(reminder: Reminder) {
    setEditing(reminder);
    setEditTitle(reminder.title);
    setEditDate(reminder.dateLabel ?? "");
    setEditTime(reminder.time ?? "");
    setEditMeridiem(reminder.meridiem ?? "AM");
    setEditRaw(reminder.rawText);
    setEditCommand("");
    setEditNote("");
    setEditRecurrenceRule(reminder.recurrenceRule ?? "");
    setEditRecurrenceLabel(reminder.recurrenceLabel ?? "");
  }

  function applyEditCommand() {
    if (!editCommand.trim()) return;
    const normalized = editCommand.toLowerCase();
    if (/\b(cancel|delete|remove)\b/.test(normalized)) {
      setCancelConfirm(true);
      return;
    }
    const parsed = parseCommand(editCommand, "Voice");
    if (parsed.dateLabel) setEditDate(parsed.dateLabel);
    if (parsed.time && parsed.meridiem) {
      setEditTime(parsed.time);
      setEditMeridiem(parsed.meridiem);
    }
    if (parsed.recurrenceRule && parsed.recurrenceLabel) {
      setEditRecurrenceRule(parsed.recurrenceRule);
      setEditRecurrenceLabel(parsed.recurrenceLabel);
      setEditDate(parsed.recurrenceLabel);
    } else if (/\b(?:stop repeating|do not repeat|don't repeat|one time|once)\b/.test(normalized)) {
      if (editDate === editRecurrenceLabel) setEditDate("");
      setEditRecurrenceRule("");
      setEditRecurrenceLabel("");
    }
    const changeTitle = normalized.match(
      /(?:change (?:the )?(?:task|title) to|rename (?:this )?to)\s+(.+)$/i,
    );
    if (changeTitle) {
      setEditTitle(
        changeTitle[1].charAt(0).toUpperCase() + changeTitle[1].slice(1),
      );
    }
    setEditNote(
      parsed.dateLabel || parsed.time || parsed.recurrenceRule
        ? "Understood. I updated the schedule below."
        : "I kept the reminder unchanged. Try “move this to tomorrow at 12 PM.”",
    );
  }

  function updateReminder() {
    if (!editing) return;
    setReminders((items) =>
      items.map((item) =>
        item.id === editing.id
          ? {
              ...item,
              title: editTitle,
              dateLabel: editDate || null,
              time: editTime || null,
              meridiem: editTime ? editMeridiem : null,
              rawText: editRaw,
              recurrenceRule: editRecurrenceRule || null,
              recurrenceLabel: editRecurrenceLabel || null,
              state:
                editDate && editTime
                  ? "scheduled"
                  : editDate
                    ? "needs_time"
                    : editTime
                      ? "needs_date"
                      : "unscheduled",
            }
          : item,
      ),
    );
    setEditing(null);
    setToast("Updated. The previous alarm was replaced.");
  }

  function deleteReminder() {
    if (!editing) return;
    setReminders((items) => items.filter((item) => item.id !== editing.id));
    setCancelConfirm(false);
    setEditing(null);
    setToast("Reminder cancelled. Its alarm was removed.");
  }

  function remindAgain() {
    const base = nextReminder ?? INITIAL_REMINDERS[0];
    setAlarmOpen(false);
    setSnoozeOpen(false);
    openEditor({
      ...base,
      rawText: `Remind me again about ${base.title.toLowerCase()}.`,
    });
    setEditCommand("Hey Atom, remind me again tomorrow at 12 PM");
    setEditNote("Say or type a new date and time.");
  }

  function completeAlarm() {
    setAlarmOpen(false);
    setSnoozeOpen(false);
    setToast("Done. Nicely handled.");
  }

  const filteredReminders =
    activeFilter === "Unscheduled"
      ? reminders.filter((item) => item.state !== "scheduled")
      : activeFilter === "Today"
        ? reminders.filter((item) => item.dateLabel?.startsWith("Today"))
        : reminders;

  return (
    <main className={`prototype theme-${theme}`}>
      <div className="ambient-orbit orbit-one" />
      <div className="ambient-orbit orbit-two" />

      <aside className="prototype-rail left-rail">
        <AtomMark variant={logoVariant} />
        <div className="rail-copy">
          <span className="eyebrow">
            <i />
            Personal reminder system
          </span>
          <h1>Nothing important slips through.</h1>
          <p>
            A focused Android prototype for Dhiren Sir. Voice-first,
            offline-first, and deliberately calm.
          </p>
        </div>
        <div className="rail-meta">
          <span>Interactive UI · Phase 1</span>
          <strong>{detectedTimezone} · detected</strong>
        </div>
      </aside>

      <section className="device-stage" aria-label="Atom mobile app prototype">
        <div className="device">
          <StatusBar />

          <div className="app-shell">
            {screen === "home" && (
              <section className="screen home-screen">
                <header className="app-header">
                  <button
                    className="brand-mark-button"
                    onClick={() => setLogoSheetOpen(true)}
                    type="button"
                    aria-label="Open Atom logo ideas"
                    data-testid="open-logo-gallery"
                  >
                    <AtomMark compact variant={logoVariant} />
                  </button>
                  <div className="header-actions">
                    <button
                      className="icon-button theme-toggle"
                      onClick={toggleTheme}
                      type="button"
                      aria-label={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
                      data-testid="theme-toggle"
                    >
                      {theme === "light" ? <Moon size={17} /> : <Sun size={17} />}
                    </button>
                    <button
                      className="icon-button alarm-preview-button"
                      onClick={() => setAlarmOpen(true)}
                      type="button"
                      aria-label="Preview alarm mode"
                      data-testid="preview-alarm"
                    >
                      <Bell size={18} />
                      <span />
                    </button>
                    <button
                      className="avatar-button"
                      onClick={() => setScreen("settings")}
                      type="button"
                      aria-label="Open profile"
                    >
                      D
                    </button>
                  </div>
                </header>

                <div className="welcome-copy">
                  <span>{todayLabel}</span>
                  <h2>
                    {greeting},
                    <br />
                    <em>{OWNER_NAME}.</em>
                  </h2>
                  <p>You have {reminders.length} things worth remembering.</p>
                  <AtomDoodle />
                </div>

                <div className="capture-card">
                  <div className="capture-orbit" aria-hidden="true">
                    <span />
                    <i />
                  </div>
                  <div className="capture-card-copy">
                    <small>QUICK CAPTURE</small>
                    <strong>What should I remember?</strong>
                  </div>
                  <div className="quick-entry">
                    <input
                      aria-label="Quick reminder"
                      placeholder="Tell Atom what to remember…"
                      onKeyDown={(event) => {
                        if (event.key !== "Enter") return;
                        const value = event.currentTarget.value;
                        setCaptureText(value);
                        setCaptureSource("Text");
                        setScreen("capture");
                        window.setTimeout(() => understand(value, "Text"), 0);
                      }}
                    />
                    <button
                      onClick={() => simulateVoice()}
                      type="button"
                      aria-label="Record a reminder"
                    >
                      <Mic size={19} />
                    </button>
                  </div>
                </div>

                <div className="section-heading">
                  <div>
                    <span className="eyebrow">
                      <i />
                      Next up
                    </span>
                  </div>
                  <button
                    onClick={() => setScreen("reminders")}
                    type="button"
                  >
                    View all
                  </button>
                </div>

                {nextReminder && (
                  <ReminderCard
                    reminder={nextReminder}
                    onClick={() => openEditor(nextReminder)}
                  />
                )}

                <div className="mini-summary">
                  <div>
                    <span className="summary-icon mint">
                      <Check size={16} />
                    </span>
                    <p>
                      <strong>4 completed</strong>
                      <small>This week</small>
                    </p>
                  </div>
                  <div>
                    <span className="summary-icon coral">
                      <Sparkles size={16} />
                    </span>
                    <p>
                      <strong>
                        {reminders.filter((r) => r.state !== "scheduled").length}{" "}
                        unscheduled
                      </strong>
                      <small>Needs a detail</small>
                    </p>
                  </div>
                </div>
              </section>
            )}

            {screen === "capture" && (
              <section className="screen capture-screen">
                <header className="sub-header">
                  <button
                    className="icon-button"
                    onClick={() => navigate("home")}
                    type="button"
                    aria-label="Back to home"
                  >
                    <ArrowLeft size={20} />
                  </button>
                  <span>New reminder</span>
                  <button
                    className="icon-button"
                    onClick={() => {
                      setCaptureText("");
                      setDraft(null);
                    }}
                    type="button"
                    aria-label="Clear reminder"
                  >
                    <X size={20} />
                  </button>
                </header>

                <div className="capture-hero">
                  <span className="eyebrow">
                    <i />
                    {isListening ? "Listening now" : "Voice or text"}
                  </span>
                  <h2>
                    Say it naturally.
                    <br />
                    <em>I’ll find the when.</em>
                  </h2>

                  <button
                    className={`voice-orb ${isListening ? "listening" : ""}`}
                    onClick={() => simulateVoice()}
                    type="button"
                    aria-label={
                      isListening ? "Stop listening" : "Start voice capture"
                    }
                  >
                    <span className="voice-ring ring-a" />
                    <span className="voice-ring ring-b" />
                    <span className="voice-core">
                      <Mic size={26} />
                    </span>
                  </button>
                  <p className="listening-label">
                    {isListening
                      ? "Listening… speak your reminder"
                      : captureSource === "Voice" && captureText
                        ? "Transcript ready"
                        : "Tap the orb to speak"}
                  </p>
                </div>

                <div className="transcript-card">
                  <div className="transcript-label">
                    <span>
                      {captureSource === "Voice" ? (
                        <Mic size={14} />
                      ) : (
                        <Edit3 size={14} />
                      )}
                      Editable {captureSource.toLowerCase()}
                    </span>
                    <small>{captureText.length}/240</small>
                  </div>
                  <textarea
                    value={captureText}
                    onChange={(event) => {
                      setCaptureText(event.target.value);
                      setCaptureSource("Text");
                      setDraft(null);
                    }}
                    placeholder="e.g. Remind me in 20 minutes to check the oven"
                    maxLength={240}
                  />
                </div>

                {!draft && (
                  <>
                    <div className="sample-strip" aria-label="Example reminders">
                      {SAMPLE_COMMANDS.map((sample, index) => (
                        <button
                          key={sample}
                          onClick={() => {
                            setCaptureText(sample);
                            setCaptureSource("Text");
                          }}
                          type="button"
                          data-testid={`sample-${index}`}
                        >
                          {sample}
                        </button>
                      ))}
                    </div>
                    <button
                      className="primary-action"
                      onClick={() => understand()}
                      type="button"
                      data-testid="understand-reminder"
                    >
                      Understand reminder
                      <Sparkles size={18} />
                    </button>
                  </>
                )}

                {draft && !followUp && (
                  <div className="review-card">
                    <div className="review-title">
                      <div>
                        <span className="success-dot">
                          <Check size={13} />
                        </span>
                        <p>
                          <small>I understood</small>
                          <strong>Review before saving</strong>
                        </p>
                      </div>
                      <button
                        onClick={() => setDraft(null)}
                        type="button"
                        aria-label="Edit transcript"
                      >
                        <Edit3 size={16} />
                      </button>
                    </div>

                    <label className="review-field">
                      <span>REMINDER</span>
                      <input
                        value={draft.title}
                        onChange={(event) =>
                          setDraft({ ...draft, title: event.target.value })
                        }
                      />
                    </label>

                    <div className="schedule-grid">
                      <button type="button">
                        <CalendarDays size={18} />
                        <span>
                          <small>Date</small>
                          <strong>{draft.dateLabel}</strong>
                        </span>
                      </button>
                      <button type="button">
                        <Clock3 size={18} />
                        <span>
                          <small>Time</small>
                          <strong>
                            {draft.time} {draft.meridiem}
                          </strong>
                        </span>
                      </button>
                    </div>

                    {draft.recurrenceLabel && (
                      <div className="recurrence-confirmation">
                        <RotateCcw size={15} />
                        <span>
                          <small>Repeats</small>
                          <strong>{draft.recurrenceLabel}</strong>
                        </span>
                      </div>
                    )}

                    {draft.relativeLabel && (
                      <div className="relative-confirmation">
                        <Zap size={15} />
                        Relative time understood: {draft.relativeLabel}
                      </div>
                    )}

                    <button
                      className="primary-action"
                      onClick={saveDraft}
                      type="button"
                    >
                      Save & schedule
                      <Bell size={18} />
                    </button>
                  </div>
                )}
              </section>
            )}

            {screen === "reminders" && (
              <section className="screen reminders-screen">
                <header className="app-header reminders-header">
                  <div>
                    <span className="eyebrow">
                      <i />
                      Your memory
                    </span>
                    <h2>Reminders</h2>
                  </div>
                  <button className="icon-button" type="button" aria-label="Search">
                    <Search size={19} />
                  </button>
                </header>

                <div className="filter-row">
                  {["Upcoming", "Today", "Unscheduled"].map((filter) => (
                    <button
                      className={activeFilter === filter ? "active" : ""}
                      onClick={() => setActiveFilter(filter)}
                      type="button"
                      key={filter}
                    >
                      {filter}
                      {filter === "Unscheduled" && (
                        <span>
                          {reminders.filter((r) => r.state !== "scheduled").length}
                        </span>
                      )}
                    </button>
                  ))}
                </div>

                <div className="reminder-list">
                  {filteredReminders.length ? (
                    filteredReminders.map((reminder) => (
                      <ReminderCard
                        key={reminder.id}
                        reminder={reminder}
                        compact
                        onClick={() => openEditor(reminder)}
                      />
                    ))
                  ) : (
                    <div className="empty-state">
                      <span>
                        <Check size={22} />
                      </span>
                      <strong>Everything is handled.</strong>
                      <p>No reminders are waiting here.</p>
                    </div>
                  )}
                </div>

                <button
                  className="floating-add"
                  onClick={() => navigate("capture")}
                  type="button"
                  aria-label="Add reminder"
                >
                  <Plus size={22} />
                </button>
              </section>
            )}

            {screen === "settings" && (
              <section className="screen settings-screen">
                <header className="app-header settings-header">
                  <div>
                    <span className="eyebrow">
                      <i />
                      Personal setup
                    </span>
                    <h2>Settings</h2>
                  </div>
                  <button
                    className="icon-button"
                    onClick={() => setScreen("home")}
                    type="button"
                    aria-label="Close settings"
                  >
                    <X size={19} />
                  </button>
                </header>

                <div className="profile-card">
                  <span className="profile-avatar">D</span>
                  <div>
                    <strong>{OWNER_NAME}</strong>
                    <small>Sole owner · this Android</small>
                  </div>
                  <CircleUserRound size={20} />
                </div>

                <div className="settings-group">
                  <span className="settings-label">AUTOMATIC</span>
                  <button className="settings-row" type="button">
                    <span className="settings-icon mint">
                      <CalendarDays size={18} />
                    </span>
                    <span>
                      <strong>Timezone</strong>
                      <small>{detectedTimezone} · detected</small>
                    </span>
                    <ChevronRight size={17} />
                  </button>
                  <button className="settings-row" type="button">
                    <span className="settings-icon coral">
                      <Volume2 size={18} />
                    </span>
                    <span>
                      <strong>Language & voice</strong>
                      <small>{detectedLocale} · detected</small>
                    </span>
                    <ChevronRight size={17} />
                  </button>
                  <button
                    className="settings-row"
                    onClick={toggleTheme}
                    type="button"
                  >
                    <span className="settings-icon sky">
                      {theme === "light" ? <Moon size={18} /> : <Sun size={18} />}
                    </span>
                    <span>
                      <strong>Appearance</strong>
                      <small>{theme === "light" ? "Light" : "Dark"} mode</small>
                    </span>
                    <span className="appearance-pill">
                      {theme === "light" ? "LIGHT" : "DARK"}
                    </span>
                  </button>
                </div>

                <div className="settings-group">
                  <span className="settings-label">RELIABILITY</span>
                  <button
                    className="settings-row"
                    onClick={() => setAlarmOpen(true)}
                    type="button"
                    data-testid="settings-alarm"
                  >
                    <span className="settings-icon dark">
                      <AlarmClock size={18} />
                    </span>
                    <span>
                      <strong>Alarm Mode</strong>
                      <small>Full-screen ring · Phase 1</small>
                    </span>
                    <span className="status-pill">ON</span>
                  </button>
                  <button className="settings-row" type="button">
                    <span className="settings-icon lime">
                      <Bell size={18} />
                    </span>
                    <span>
                      <strong>Notification health</strong>
                      <small>All permissions ready</small>
                    </span>
                    <span className="health-dot" />
                  </button>
                </div>

                <div className="settings-group">
                  <span className="settings-label">VOICE FALLBACK</span>
                  <button
                    className="settings-row toggle-row"
                    onClick={() => setCloudFallback((value) => !value)}
                    type="button"
                    aria-pressed={cloudFallback}
                    data-testid="cloud-fallback"
                  >
                    <span className="settings-icon sky">
                      <Cloud size={18} />
                    </span>
                    <span>
                      <strong>OpenAI transcription</strong>
                      <small>Uses paid API credits when enabled</small>
                    </span>
                    <span className={`switch ${cloudFallback ? "on" : ""}`}>
                      <i />
                    </span>
                  </button>
                  <p className="settings-footnote">
                    Native on-device speech stays primary. Cloud fallback is only
                    used after a failed or low-confidence transcript.
                  </p>
                </div>

                <div className="settings-group phrase-group">
                  <span className="settings-label">UNDERSTOOD PREFIXES</span>
                  <div className="phrase-cloud">
                    {PREFIXES.slice(0, 9).map((prefix) => (
                      <span key={prefix}>{prefix}</span>
                    ))}
                    <span>+{PREFIXES.length - 9} more</span>
                  </div>
                </div>
              </section>
            )}

            <BottomNav screen={screen} onChange={navigate} />
          </div>

          {followUp && draft && (
            <div className="modal-layer followup-layer" role="dialog" aria-modal="true">
              <button
                className="modal-backdrop"
                onClick={() => setFollowUp(false)}
                type="button"
                aria-label="Close follow-up"
              />
              <section className="bottom-sheet followup-sheet">
                <div className="sheet-grabber" />
                <div className="sheet-heading">
                  <span className="attention-icon">
                    <Sparkles size={18} />
                  </span>
                  <div>
                    <small>ONE QUICK FOLLOW-UP</small>
                    <h3>
                      {!draft.dateLabel && !draft.time
                        ? "When should I remind you?"
                        : !draft.time
                          ? `What time ${draft.dateLabel?.split(" · ")[0].toLowerCase()}?`
                          : "Which date should I use?"}
                    </h3>
                  </div>
                  <button
                    onClick={() => setFollowUp(false)}
                    type="button"
                    aria-label="Close"
                  >
                    <X size={19} />
                  </button>
                </div>

                <p className="followup-context">
                  <span>“{draft.title}”</span>
                  I saved your task. Add the missing schedule now, or keep it in
                  Unscheduled.
                </p>

                <div className="follow-voice-field">
                  <button
                    className={followListening ? "listening" : ""}
                    onClick={() => {
                      setFollowListening(true);
                      window.setTimeout(() => {
                        const phrase =
                          !draft.dateLabel && draft.time
                            ? "tomorrow"
                            : "tomorrow at 6:30 PM";
                        setFollowText(phrase);
                        setFollowListening(false);
                      }, 700);
                    }}
                    type="button"
                    aria-label="Speak missing date and time"
                  >
                    <Mic size={18} />
                  </button>
                  <input
                    value={followText}
                    onChange={(event) => setFollowText(event.target.value)}
                    placeholder={
                      followListening
                        ? "Listening…"
                        : "Say or type “tomorrow at 6:30 PM”"
                    }
                    data-testid="followup-input"
                  />
                </div>

                {!draft.dateLabel && (
                  <div className="picker-block">
                    <span>DATE</span>
                    <div className="choice-row">
                      {["Today", "Tomorrow", "Fri, Jul 31"].map((label) => (
                        <button
                          className={
                            (followText || "").toLowerCase().includes(
                              label.split(",")[0].toLowerCase(),
                            )
                              ? "selected"
                              : ""
                          }
                          onClick={() =>
                            setFollowText((value) =>
                              `${label === "Fri, Jul 31" ? "Friday" : label.toLowerCase()}${value.match(/ at .*/i)?.[0] ?? ""}`,
                            )
                          }
                          type="button"
                          key={label}
                        >
                          {label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {!draft.time && (
                  <div className="picker-block">
                    <span>TIME · 12-HOUR FORMAT</span>
                    <div className="time-picker">
                      <Clock3 size={18} />
                      <input
                        value={
                          followText.match(
                            /\b\d{1,2}(?::\d{2})?\s*(?:AM|PM)\b/i,
                          )?.[0] ?? "6:30"
                        }
                        onChange={(event) => {
                          const suffix =
                            followText.toUpperCase().includes(" AM") ? "AM" : "PM";
                          const base = followText
                            .replace(
                              /\b\d{1,2}(?::\d{2})?\s*(?:AM|PM)?\b/i,
                              "",
                            )
                            .trim();
                          setFollowText(
                            `${base || "tomorrow"} at ${event.target.value} ${suffix}`,
                          );
                        }}
                        aria-label="Reminder time in 12-hour format"
                      />
                      <button
                        className={
                          followText.toUpperCase().includes(" AM")
                            ? "selected"
                            : ""
                        }
                        onClick={() =>
                          setFollowText((value) =>
                            `${value.replace(/\s+(?:AM|PM)$/i, "")} AM`,
                          )
                        }
                        type="button"
                      >
                        AM
                      </button>
                      <button
                        className={
                          !followText.toUpperCase().includes(" AM")
                            ? "selected"
                            : ""
                        }
                        onClick={() =>
                          setFollowText((value) =>
                            `${value.replace(/\s+(?:AM|PM)$/i, "") || "tomorrow at 6:30"} PM`,
                          )
                        }
                        type="button"
                      >
                        PM
                      </button>
                    </div>
                  </div>
                )}

                <button
                  className="primary-action"
                  onClick={applyFollowUp}
                  type="button"
                  data-testid="followup-continue"
                >
                  Continue to review
                  <ChevronRight size={18} />
                </button>
                <button
                  className="text-action"
                  onClick={saveWithoutSchedule}
                  type="button"
                >
                  Save to Unscheduled instead
                </button>
              </section>
            </div>
          )}

          {logoSheetOpen && (
            <div
              className="modal-layer logo-sheet-layer"
              role="dialog"
              aria-modal="true"
              aria-label="Atom logo ideas"
              data-testid="logo-gallery"
            >
              <button
                className="modal-backdrop"
                onClick={() => setLogoSheetOpen(false)}
                type="button"
                aria-label="Close logo ideas"
              />
              <section className="bottom-sheet logo-sheet">
                <div className="sheet-grabber" />
                <div className="logo-sheet-heading">
                  <div>
                    <span className="eyebrow">
                      <i />
                      Brand playground
                    </span>
                    <h3>Choose Atom’s mark.</h3>
                    <p>
                      The original remains the default. Tap any direction to
                      preview it across the app.
                    </p>
                  </div>
                  <button
                    onClick={() => setLogoSheetOpen(false)}
                    type="button"
                    aria-label="Close"
                  >
                    <X size={19} />
                  </button>
                </div>

                <div className="logo-gallery-grid">
                  {LOGO_OPTIONS.map((option) => (
                    <button
                      className={logoVariant === option.id ? "active" : ""}
                      onClick={() => setLogoVariant(option.id)}
                      type="button"
                      key={option.id}
                      aria-pressed={logoVariant === option.id}
                      data-testid={`logo-gallery-${option.id}`}
                    >
                      <AtomMark
                        compact
                        variant={option.id}
                        wordmark={false}
                      />
                      <span>{option.label}</span>
                      {option.id === "original" && <small>Current</small>}
                    </button>
                  ))}
                </div>

                <button
                  className="primary-action"
                  onClick={() => setLogoSheetOpen(false)}
                  type="button"
                >
                  Keep this preview
                  <Check size={18} />
                </button>
              </section>
            </div>
          )}

          {editing && (
            <div className="modal-layer editor-layer" role="dialog" aria-modal="true">
              <button
                className="modal-backdrop"
                onClick={() => setEditing(null)}
                type="button"
                aria-label="Close reminder editor"
              />
              <section className="bottom-sheet editor-sheet">
                <div className="sheet-grabber" />
                <div className="editor-heading">
                  <div>
                    <span className="eyebrow">
                      <i />
                      Edit reminder
                    </span>
                    <h3>Change anything.</h3>
                  </div>
                  <button
                    onClick={() => setEditing(null)}
                    type="button"
                    aria-label="Close"
                  >
                    <X size={20} />
                  </button>
                </div>

                <div className="voice-edit">
                  <button
                    onClick={() =>
                      setEditCommand("Hey Atom, change this to tomorrow at 12 PM")
                    }
                    type="button"
                    aria-label="Speak an edit"
                  >
                    <Mic size={18} />
                  </button>
                  <input
                    value={editCommand}
                    onChange={(event) => setEditCommand(event.target.value)}
                    placeholder="Say “move this to tomorrow at 12 PM”"
                    data-testid="edit-command"
                  />
                  <button
                    onClick={applyEditCommand}
                    type="button"
                    data-testid="apply-edit-command"
                  >
                    Apply
                  </button>
                </div>
                {editNote && <p className="edit-note">{editNote}</p>}

                <label className="editor-field">
                  <span>REMINDER</span>
                  <input
                    value={editTitle}
                    onChange={(event) => setEditTitle(event.target.value)}
                  />
                </label>

                <label className="editor-field transcript-edit">
                  <span>ORIGINAL TEXT · EDITABLE</span>
                  <textarea
                    value={editRaw}
                    onChange={(event) => setEditRaw(event.target.value)}
                  />
                </label>

                <div className="editor-schedule">
                  <label>
                    <span>DATE</span>
                    <div>
                      <CalendarDays size={17} />
                      <input
                        value={editDate}
                        placeholder="Choose date"
                        onChange={(event) => setEditDate(event.target.value)}
                      />
                    </div>
                  </label>
                  <label>
                    <span>TIME · 12-HOUR</span>
                    <div className="inline-time">
                      <input
                        value={editTime}
                        placeholder="6:30"
                        onChange={(event) => setEditTime(event.target.value)}
                      />
                      <button
                        className={editMeridiem === "AM" ? "selected" : ""}
                        onClick={() => setEditMeridiem("AM")}
                        type="button"
                      >
                        AM
                      </button>
                      <button
                        className={editMeridiem === "PM" ? "selected" : ""}
                        onClick={() => setEditMeridiem("PM")}
                        type="button"
                      >
                        PM
                      </button>
                    </div>
                  </label>
                </div>

                <label className="editor-field recurrence-field">
                  <span>REPEAT</span>
                  <div className="recurrence-select-wrap">
                    <RotateCcw size={16} />
                    <select
                      value={editRecurrenceRule}
                      onChange={(event) => {
                        const option = RECURRENCE_OPTIONS.find(
                          (item) => item.rule === event.target.value,
                        );
                        if (!event.target.value && editDate === editRecurrenceLabel) {
                          setEditDate("");
                        }
                        setEditRecurrenceRule(event.target.value);
                        setEditRecurrenceLabel(option?.label ?? "");
                        if (option?.rule) setEditDate(option.label);
                      }}
                      aria-label="Reminder recurrence"
                    >
                      {RECURRENCE_OPTIONS.map((option) => (
                        <option key={option.label} value={option.rule}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </label>

                <div className="replacement-note">
                  <RotateCcw size={16} />
                  Saving replaces the previous alarm. You will never receive both.
                </div>

                <div className="editor-actions">
                  <button
                    className="delete-action"
                    onClick={() => setCancelConfirm(true)}
                    type="button"
                    aria-label="Cancel reminder"
                  >
                    <Trash2 size={18} />
                  </button>
                  <button
                    className="primary-action"
                    onClick={updateReminder}
                    type="button"
                    data-testid="update-reminder"
                  >
                    Update reminder
                    <Check size={18} />
                  </button>
                </div>
              </section>

              {cancelConfirm && (
                <div className="confirmation-card" role="alertdialog">
                  <span className="danger-icon">
                    <Trash2 size={20} />
                  </span>
                  <h4>Cancel this reminder?</h4>
                  <p>The scheduled Android alarm will also be removed.</p>
                  <div>
                    <button
                      onClick={() => setCancelConfirm(false)}
                      type="button"
                    >
                      Keep it
                    </button>
                    <button onClick={deleteReminder} type="button">
                      Cancel reminder
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {alarmOpen && (
            <div
              className="alarm-mode"
              role="dialog"
              aria-modal="true"
              data-testid="alarm-mode"
            >
              <div className="alarm-top">
                <AtomMark compact variant={logoVariant} />
                <button
                  onClick={() => setAlarmOpen(false)}
                  type="button"
                  aria-label="Close alarm preview"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="alarm-content">
                <div className="alarm-visual">
                  <span className="alarm-ring ring-one" />
                  <span className="alarm-ring ring-two" />
                  <span className="alarm-ring ring-three" />
                  <span className="alarm-bell">
                    <Bell size={34} />
                  </span>
                </div>
                <span className="alarm-kicker">ATOM REMINDER</span>
                <time>
                  12:00 <em>AM</em>
                </time>
                <h3>{nextReminder?.title ?? "Send the proposal"}</h3>
                <p>Tomorrow · Wednesday, 29 July</p>
              </div>

              {!snoozeOpen ? (
                <div className="alarm-actions">
                  <button onClick={completeAlarm} type="button">
                    <span>
                      <Check size={20} />
                    </span>
                    Done
                  </button>
                  <button onClick={() => setSnoozeOpen(true)} type="button">
                    <span>
                      <Clock3 size={20} />
                    </span>
                    Snooze
                  </button>
                  <button onClick={remindAgain} type="button">
                    <span>
                      <RotateCcw size={20} />
                    </span>
                    Remind me again
                  </button>
                </div>
              ) : (
                <div className="snooze-panel">
                  <div>
                    <span>SNOOZE FOR</span>
                    <button
                      onClick={() => setSnoozeOpen(false)}
                      type="button"
                      aria-label="Close snooze options"
                    >
                      <X size={18} />
                    </button>
                  </div>
                  <section>
                    {["10 min", "20 min", "1 hour", "Custom"].map((option) => (
                      <button
                        key={option}
                        onClick={() => {
                          setAlarmOpen(false);
                          setSnoozeOpen(false);
                          setToast(`Snoozed for ${option.toLowerCase()}.`);
                        }}
                        type="button"
                      >
                        {option}
                      </button>
                    ))}
                  </section>
                </div>
              )}
            </div>
          )}

          {toast && (
            <div className="toast" role="status">
              <span>
                <Check size={15} />
              </span>
              {toast}
            </div>
          )}
        </div>
      </section>

      <aside className="prototype-rail right-rail">
        <div className="flow-card">
          <span className="eyebrow">
            <i />
            Try the complete flow
          </span>
          <h3>Prototype controls</h3>
          <p>Use these shortcuts to inspect Atom’s edge cases.</p>
          <div className="flow-actions">
            <button
              onClick={() =>
                simulateVoice("Please remind me to call Rhea")
              }
              type="button"
            >
              <span>01</span>
              Missing date + time
              <ChevronRight size={16} />
            </button>
            <button
              onClick={() =>
                simulateVoice("Remind me in 20 minutes to check the oven")
              }
              type="button"
            >
              <span>02</span>
              Relative time
              <ChevronRight size={16} />
            </button>
            <button
              onClick={() => openEditor(INITIAL_REMINDERS[0])}
              type="button"
            >
              <span>03</span>
              Voice reschedule
              <ChevronRight size={16} />
            </button>
            <button
              onClick={() =>
                simulateVoice(
                  "Every weekday at 9 AM remind me to review my priorities",
                )
              }
              type="button"
            >
              <span>04</span>
              Recurring reminder
              <ChevronRight size={16} />
            </button>
            <button onClick={() => setAlarmOpen(true)} type="button">
              <span>05</span>
              Full-screen alarm
              <ChevronRight size={16} />
            </button>
          </div>
        </div>

        <div className="logo-lab-card">
          <div className="logo-lab-heading">
            <span>
              <small>ATOM MARK STUDIES</small>
              <strong>Compare the character.</strong>
            </span>
            <em>{LOGO_OPTIONS.length} directions</em>
          </div>
          <div className="logo-option-grid">
            {LOGO_OPTIONS.map((option) => (
              <button
                className={logoVariant === option.id ? "active" : ""}
                onClick={() => setLogoVariant(option.id)}
                type="button"
                key={option.id}
                aria-pressed={logoVariant === option.id}
                aria-label={`Preview ${option.label} Atom logo`}
                data-testid={`logo-rail-${option.id}`}
              >
                <AtomMark
                  compact
                  variant={option.id}
                  wordmark={false}
                />
                <span>{option.label}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="principle-card">
          <div className="principle-icon">
            <Zap size={18} />
          </div>
          <p>
            <strong>One calm rule</strong>
            Atom never invents a missing date, time, or AM/PM.
          </p>
        </div>
      </aside>
    </main>
  );
}
